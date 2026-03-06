package ro.simavi.sphinx.ad.ws.grafana;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.simavi.sphinx.ad.services.grafana.GrafanaService;
import ro.simavi.sphinx.model.grafana.GrafanaDatasource;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

@RestController
@RequestMapping("/grafana/config")
public class ConfigGrafanaController {

    private final GrafanaService grafanaService;

    public ConfigGrafanaController(GrafanaService grafanaService){
        this.grafanaService = grafanaService;
    }

    @ApiIgnore
    @GetMapping(value = "/api/datasources")
    public String getDataSources() {
        return grafanaService.getDataSourcesString();
    }

    @ApiIgnore
    @GetMapping(
            value = "/api/datasources/export",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<byte[]> getDataSourcesExport(){
        HttpHeaders header = new HttpHeaders();
        header.set("Content-Disposition", "attachment; filename=" + "data-source-"+new Date().getTime());

        return new ResponseEntity<>(grafanaService.getDataSourcesString().getBytes(), header, HttpStatus.OK);
    }

    @ApiIgnore
    @PostMapping(value = "/api/datasources")
    public ResponseEntity<String> uploadNewFile(@NotNull @RequestParam("file") MultipartFile multipartFile) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        /*try {
            JSONObject json = new JSONObject("{list:"+sb.toString()+"}")
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */

        ObjectMapper m = new ObjectMapper();
        GrafanaDatasource[] grafanaDatasourceList = m.readValue(sb.toString(), GrafanaDatasource[].class);

        String result = grafanaService.importDataSource(grafanaDatasourceList);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
