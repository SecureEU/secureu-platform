package ro.simavi.sphinx.model.enums;

import lombok.Getter;

@Getter
public enum DnsType {

    QUERY("0"),

    ANSWER("1");

    private String value;

    DnsType(String value){
        this.value = value;
    }

}
