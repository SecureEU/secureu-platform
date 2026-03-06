package ro.simavi.sphinx.ad.dpi.flow.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum MethodHttp {

    GET(20),
    HEAD(21),
    POST(22),
    PUT(23),
    PATCH(24),
    DELETE(25);

    private int code;

    MethodHttp(int code){
        this.code = code;

    }

}
