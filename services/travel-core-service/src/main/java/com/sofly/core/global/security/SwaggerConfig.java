package com.sofly.core.global.security;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtScheme = "bearerAuth";

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(new Info()
                        .title("Sofly Core API")
                        .description("Sofly Core 백엔드 API 문서")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList(jwtScheme))
                .components(new Components()
                        .addSecuritySchemes(jwtScheme, new SecurityScheme()
                                .name(jwtScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer tagsOrderCustomizer() {
        return openApi -> openApi.setTags(List.of(
                new Tag().name("Auth").description("인증 관련 API"),
                new Tag().name("UserProfile").description("프로필"),
                new Tag().name("Workspace").description("워크스페이스 생성·관리"),
                new Tag().name("Workspace Invite").description("워크스페이스 초대·공유"),
                new Tag().name("Workspace Member").description("워크스페이스 멤버 관리"),
                new Tag().name("Workspace Flight").description("워크스페이스 항공편 저장·조회"),
                new Tag().name("Chat").description("AI 여행 플래너 채팅"),
                new Tag().name("Schedule").description("일정 관리 API")
        ));
    }
}
