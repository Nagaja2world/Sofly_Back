package com.sofly.core.domain.workspace.repository;

import com.sofly.core.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

}
