package com.emilhuseynov.accesspass.staff;

import com.emilhuseynov.accesspass.staff.StaffDtos.CreateStaffRequest;
import com.emilhuseynov.accesspass.staff.StaffDtos.StaffResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
class StaffController {

    private final StaffService staffService;

    StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return StaffResponse.from(staffService.create(request.username(), request.password(), request.role()));
    }

    @GetMapping
    List<StaffResponse> list() {
        return staffService.list().stream().map(StaffResponse::from).toList();
    }

    @PostMapping("/{staffId}/disable")
    StaffResponse disable(@PathVariable Long staffId) {
        return StaffResponse.from(staffService.disable(staffId));
    }
}
