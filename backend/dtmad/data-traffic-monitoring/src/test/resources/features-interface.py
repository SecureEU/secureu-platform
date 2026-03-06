from nfstream import NFStreamer
from nfstream import NFStreamer, NFPlugin

my_awesome_streamer = NFStreamer(source="br-0ecee535f3be")

for flow in my_awesome_streamer:
    print(flow) # print it, append to pandas Dataframe or whatever you want :)!

total_flows_count = my_awesome_streamer.to_csv(path="/home/sphinx/nfstream/finalxx", columns_to_anonymize=[], flows_per_file=0)

print(str(total_flows_count))