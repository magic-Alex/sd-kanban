package com.sdkanban.project.service;

import com.sdkanban.project.dto.AddProjectMemberRequest;
import com.sdkanban.project.dto.CreateProjectRequest;
import com.sdkanban.project.dto.ProjectMemberResponse;
import com.sdkanban.project.dto.ProjectResponse;
import com.sdkanban.project.dto.TransferProjectOwnerRequest;
import com.sdkanban.project.entity.ProjectMember;

import java.util.List;

public interface ProjectService {
    ProjectResponse create(CreateProjectRequest request, Long currentUserId);

    List<ProjectResponse> listForUser(Long currentUserId);

    ProjectResponse detail(Long projectId, Long currentUserId);

    List<ProjectMemberResponse> listMembers(Long projectId, Long currentUserId);

    ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request, Long currentUserId);

    void removeMember(Long projectId, Long userId, Long currentUserId);

    ProjectResponse transferOwner(Long projectId, TransferProjectOwnerRequest request, Long currentUserId);

    ProjectMember requireMember(Long projectId, Long userId);

    ProjectMember requireOwner(Long projectId, Long userId);
}
