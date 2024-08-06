package com.c108.meetz.dto.response;

import com.c108.meetz.domain.BlackList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class BlackListInfoListResponseDto {
    List<BlackListInfo> blackList;

    private BlackListInfoListResponseDto(List<BlackListInfo> blackList){
        this.blackList = blackList;
    }

    public static BlackListInfoListResponseDto from(List<BlackListInfo> blackList){
        return new BlackListInfoListResponseDto(blackList);
    }

    @Getter
    @Setter
    public static class BlackListInfo{
        int blacklistId;
        String name;
        String email;
        String phone;

        public static BlackListInfo from(BlackList blackList){
            BlackListInfo blackListInfo = new BlackListInfo();
            blackListInfo.setBlacklistId(blackList.getBlacklistId());
            blackListInfo.setName(blackList.getName());
            blackListInfo.setEmail(blackList.getEmail());
            blackListInfo.setPhone(blackList.getPhone());
            return blackListInfo;
        }

    }

}
