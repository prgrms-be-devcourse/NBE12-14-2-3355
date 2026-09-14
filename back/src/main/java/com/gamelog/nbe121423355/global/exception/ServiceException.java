package com.gamelog.nbe121423355.global.exception;

import com.gamelog.nbe121423355.global.dto.RsData;
import lombok.Getter;

public class ServiceException extends RuntimeException{
  @Getter
  private RsData rsData;

  public ServiceException(String resultCode, String message){
    super(message);
    this.rsData = new RsData(
            resultCode,
            message
    );
  }

  public String getResultCode(){
    return rsData.getResultCode();
  }

  public String getMsg(){
    return rsData.getMsg();
  }
}

