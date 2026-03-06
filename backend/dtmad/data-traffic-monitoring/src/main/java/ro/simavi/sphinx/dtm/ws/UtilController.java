package ro.simavi.sphinx.dtm.ws;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.entities.DtmCmdEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.DtmCmdRepository;
import ro.simavi.sphinx.dtm.model.MessageModel;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.model.event.DtmCmdEventModel;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/helper/util")
public class UtilController {

    private static final int POLL_TIMEOUT_MS = 200;

    @Autowired
    private Environment environment;

    private static final Logger logger = LoggerFactory.getLogger(UtilController.class);

    @Autowired
    private AdminClient adminClient;

    @Autowired
    private DtmCmdRepository dtmCmdRepository;

    @Autowired
    private MessagingSystemService messagingSystemService;

    @Autowired
    private KafkaConsumer<String,String> kafkaConsumer;

    @GetMapping(value = "/getActiveProfiles")
    public String[] getActiveProfiles() throws IOException {
        return environment.getActiveProfiles();
    }

    @GetMapping(value = "/getTopics")
    public List<String> getTopics() throws IOException, ExecutionException, InterruptedException {

        ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
        listTopicsOptions.listInternal(true);

        List<String> topicList = new ArrayList<>();
        for (TopicListing topicListing : adminClient.listTopics().listings().get()) {
            String topicName = topicListing.name();
            if (topicName.startsWith("dtm-") || topicName.startsWith("ad-")) {
                topicList.add(topicName);
            }
        }

        return topicList;
    }

    @GetMapping(value = "/getLastCmd")
    public Iterable<DtmCmdEntity> getLastDtmCmd() throws IOException {
        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "id");
        return dtmCmdRepository.findWithPageable(pageable);
    }

    @GetMapping(value = "/sendKafkaCmdTest")
    public ResponseModel sendKafkaCmdTest() throws IOException {
        try {
            DtmCmdEventModel dtmCmdEventModel = getDtmCmdEventModel();
            messagingSystemService.sendDtmCmd(dtmCmdEventModel);
            return new ResponseModel("test: " + dtmCmdEventModel.getUuid(), Boolean.TRUE);
        } catch (Exception e) {
            return new ResponseModel(e.getMessage(), Boolean.FALSE);
        }
    }

    private DtmCmdEventModel getDtmCmdEventModel() {
        UUID uuid = UUID.randomUUID();
        DtmCmdEventModel dtmCmdEventModel = new DtmCmdEventModel();
        dtmCmdEventModel.setHostname(NetworkHelper.getHostName());
        dtmCmdEventModel.setUsername(NetworkHelper.getUsername());
        dtmCmdEventModel.setInstanceKey(NetworkHelper.getInstanceKey());
        dtmCmdEventModel.setUuid(uuid.toString());
        dtmCmdEventModel.setMethod("-");
        dtmCmdEventModel.setUrl("/sendKafkaCmdTest");
        dtmCmdEventModel.setCreatedDate(LocalDateTime.now());
        return dtmCmdEventModel;
    }

    @GetMapping(value = "/getLastMessagesForTopic/{kafkaTopicName}")
    public List<MessageModel> getTopic(@PathVariable(name = "kafkaTopicName")String kafkaTopicName){

        if (!kafkaTopicName.startsWith("dtm-") && !kafkaTopicName.startsWith("ad-")){
            return Collections.emptyList();
        }

        int count = 100;

        final List<PartitionInfo> partitionInfoSet = kafkaConsumer.partitionsFor(kafkaTopicName);

        final Collection<TopicPartition> partitions = partitionInfoSet
                .stream()
                .map(partitionInfo -> new TopicPartition(partitionInfo.topic(), partitionInfo.partition()))
                .collect(Collectors.toList());
        kafkaConsumer.assign(partitions);

        final Map<TopicPartition, Long> latestOffsets = kafkaConsumer.endOffsets(partitions);

        for (TopicPartition partition : partitions) {
            final long latestOffset = Math.max(0, latestOffsets.get(partition) - 1);
            kafkaConsumer.seek(partition, Math.max(0, latestOffset - count));
        }

        final long totalCount = count * partitions.size();
        final Map<TopicPartition, List<ConsumerRecord<String,String>>> rawRecords = partitions
                .stream()
                .collect(Collectors.toMap(p -> p , p -> new ArrayList<>(count)));

        Boolean moreRecords = true;
        while (rawRecords.size() < totalCount && moreRecords) {
            final ConsumerRecords<String, String> polled = kafkaConsumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS));

            moreRecords = false;
            for (TopicPartition partition : polled.partitions()) {
                List<ConsumerRecord<String, String>> records = polled.records(partition);
                if (!records.isEmpty()) {
                    rawRecords.get(partition).addAll(records);
                    moreRecords = records.get(records.size() - 1).offset() < latestOffsets.get(partition) - 1;
                }
            }
        }

        List<ConsumerRecord<String,String>> list = rawRecords
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(rec -> new ConsumerRecord<String, String>(
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.timestamp(),
                        rec.timestampType(),
                        0L,
                        rec.serializedKeySize(),
                        rec.serializedValueSize(),
                        rec.key(),
                        rec.value(),
                        rec.headers(),
                        rec.leaderEpoch()))
                .collect(Collectors.toList());

        return getMessages(list);
    }

    public List<MessageModel> getMessages(List<ConsumerRecord<String,String>> records) {

        if (records != null) {
            final List<MessageModel> messageModelList = new ArrayList<MessageModel>();
            for (ConsumerRecord<String,String> record : records) {
                final MessageModel messageModel = new MessageModel();
                messageModel.setPartition(record.partition());
                messageModel.setOffset(record.offset());
                messageModel.setKey(record.key());
                messageModel.setMessage(record.value());
                messageModel.setHeaders(headersToMap(record.headers()));
                messageModel.setTimestamp(new Date(record.timestamp()));
                messageModelList.add(messageModel);
            }
            return messageModelList;
        } else {
            return Collections.emptyList();
        }
    }

    private static Map<String, String> headersToMap(Headers headers) {
        final TreeMap map = new TreeMap<String, String>();
        for (Header header : headers) {
            final byte[] value = header.value();
            map.put(header.key(), (value == null) ? null : new String(value));
        }
        return map;
    }

}
