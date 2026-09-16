package com.gamelog.nbe121423355.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User {

    private final Long id;
    private final String email;
    private final String nickname;

    public SecurityUser(Long id, String email, String nickname, Collection<? extends GrantedAuthority> authorities) {
        super(email, "", authorities);
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }
}
