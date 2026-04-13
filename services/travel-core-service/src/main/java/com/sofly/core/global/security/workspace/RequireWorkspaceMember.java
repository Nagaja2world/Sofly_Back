package com.sofly.core.global.security.workspace;

import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireWorkspaceMember {
    MemberRole minRole() default MemberRole.VIEWER;
}
