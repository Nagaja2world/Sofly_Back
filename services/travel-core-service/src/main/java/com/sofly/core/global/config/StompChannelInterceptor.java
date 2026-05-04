package com.sofly.core.global.config;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import com.sofly.core.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        // ✅ getUserId()로 userId 추출 후 직접 Authentication 생성
                        Long userId = jwtTokenProvider.getUserId(token);
                        
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new SoflyException(ErrorCode.USER_NOT_FOUND));

                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                userId,   // principal → SecurityUtils.getCurrentUserId()가 꺼내 씀
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        // SecurityContextHolder.getContext().setAuthentication(auth);
                        accessor.setUser(auth);

                        accessor.getSessionAttributes().put("nickname", user.getNickname());
                    }
                } catch (Exception e) {
                    log.warn("WebSocket JWT 인증 실패: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
