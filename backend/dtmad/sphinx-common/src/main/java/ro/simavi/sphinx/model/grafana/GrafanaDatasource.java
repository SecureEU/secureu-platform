package ro.simavi.sphinx.model.grafana;

import lombok.Getter;
import lombok.Setter;

/*
    "id": 1,
    "orgId": 1,
    "name": "[AD] BlackWeb JSON ",
    "type": "simpod-json-datasource",
    "typeLogoUrl": "public/img/icn-datasource.svg",
    "access": "direct",
    "url": "http://localhost:8088/sphinx/ad/api/getBlackWebAlertList",
    "password": "",
    "user": "",
    "database": "",
    "basicAuth": false,
    "isDefault": false,
    "jsonData": {},
    "readOnly": false
 */
@Getter
@Setter
public class GrafanaDatasource {
     private Long id;
     private Long orgId;
     private String name;
     private String type;
     private String typeLogoUrl;
     private String access;
     private String url;
     private String password;
     private String user;
     private String database;
     private Boolean basicAuth;
     private Boolean isDefault;
     private Object jsonData;
     private Boolean readOnly;
}
