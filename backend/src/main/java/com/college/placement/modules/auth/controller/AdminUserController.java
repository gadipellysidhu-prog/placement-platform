package com.college.placement.modules.auth.controller;

import com.college.placement.modules.auth.domain.AccountStatus;
import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.AssignRoleRequest;
import com.college.placement.modules.auth.dto.InviteUserRequest;
import com.college.placement.modules.auth.dto.MessageResponse;
import com.college.placement.modules.auth.dto.UserResponse;
import com.college.placement.modules.auth.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User Administration", description = "Administrative user & role management (admin)")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService service;

    public AdminUserController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List/search/filter users (paginated)")
    public ResponseEntity<Page<UserResponse>> list(@RequestParam(required = false) Role role,
                                                   @RequestParam(required = false) AccountStatus status,
                                                   @RequestParam(required = false) String query,
                                                   Pageable pageable) {
        Page<AppUser> users = service.list(role, status, query, pageable);
        // Resolved once for the whole page rather than per row.
        Map<UUID, Instant> lastActivity = service.lastActivityFor(
                users.getContent().stream().map(AppUser::getId).toList());
        return ResponseEntity.ok(users.map(u -> UserResponse.from(u, lastActivity.get(u.getId()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(withActivity(service.getById(id)));
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite a new privileged user (sends an activation email)")
    public ResponseEntity<MessageResponse> invite(@Valid @RequestBody InviteUserRequest req) {
        service.invite(req.email(), req.role());
        return ResponseEntity.accepted().body(new MessageResponse("Invitation sent."));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable (activate) an account")
    public ResponseEntity<UserResponse> enable(@PathVariable UUID id) {
        return ResponseEntity.ok(withActivity(service.enable(id)));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable (soft-deactivate) an account")
    public ResponseEntity<UserResponse> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(withActivity(service.disable(id)));
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "Lock an account")
    public ResponseEntity<UserResponse> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(withActivity(service.lock(id)));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock an account")
    public ResponseEntity<UserResponse> unlock(@PathVariable UUID id) {
        return ResponseEntity.ok(withActivity(service.unlock(id)));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<UserResponse> assignRole(@PathVariable UUID id,
                                                   @Valid @RequestBody AssignRoleRequest req) {
        return ResponseEntity.ok(withActivity(service.assignRole(id, req.role())));
    }

    /** Keeps single-user responses consistent with the list projection. */
    private UserResponse withActivity(AppUser user) {
        return UserResponse.from(user, service.lastActivityFor(user.getId()));
    }
}
