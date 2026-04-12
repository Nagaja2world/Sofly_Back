package com.sofly.core.global.security.workspace;

import com.sofly.core.domain.workspace.entity.WorkspaceMember;
import com.sofly.core.domain.workspace.entity.WorkspaceMember.MemberRole;
import com.sofly.core.domain.workspace.repository.WorkspaceMemberRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import com.sofly.core.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * {@link RequireWorkspaceMember} 애노테이션이 붙은 메서드에 대해 워크스페이스 멤버 권한을 검증하는 Aspect.
 *
 * <p><b>주의:</b> 적용 대상 메서드는 반드시 {@code workspaceId}라는 이름의 {@code Long} 파라미터를 포함해야 합니다.
 * 해당 파라미터가 없으면 런타임에 {@link IllegalStateException}이 발생합니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class WorkspaceMemberAspect {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Before("@annotation(require)")
    public void checkWorkspaceMembership(JoinPoint jp, RequireWorkspaceMember require) {
        Long workspaceId = extractWorkspaceId(jp);
        Long userId = SecurityUtils.getCurrentUserId();

        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED));

        if (require.minRole() != MemberRole.VIEWER && !hasRequiredRole(member.getRole(), require.minRole())) {
            throw new SoflyException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    private Long extractWorkspaceId(JoinPoint jp) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = jp.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if ("workspaceId".equals(parameters[i].getName())) {
                return (Long) args[i];
            }
        }

        throw new IllegalStateException(
                "@RequireWorkspaceMember가 붙은 메서드에 'workspaceId' 파라미터가 없습니다: "
                + signature.getMethod().getName()
        );
    }

    /** minRole 이상인지 확인 — enum 선언 순서: OWNER(0) > EDITOR(1) > VIEWER(2) */
    private boolean hasRequiredRole(MemberRole actual, MemberRole required) {
        return actual.ordinal() <= required.ordinal();
    }
}
