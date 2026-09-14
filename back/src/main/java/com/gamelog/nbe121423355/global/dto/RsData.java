package com.gamelog.nbe121423355.global.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> {

    private String resultCode;
    private String msg;
    private T data;

    public RsData(String resultCode, String msg){
        this.resultCode=resultCode;
        this.msg=msg;
        this.data=null;
    }

    @JsonIgnore
    public int getStatusCode(){
        return Integer.parseInt(resultCode.split("-")[0]);
    }
}
