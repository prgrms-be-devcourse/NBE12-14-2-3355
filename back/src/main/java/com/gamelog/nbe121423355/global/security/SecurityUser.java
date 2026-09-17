package com.gamelog.nbe121423355.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User {

    private final Long id;


    public SecurityUser(Long id, Collection<? extends GrantedAuthority> authorities) {
        super(String.valueOf(id), "", authorities);
        this.id = id;
    }
}
