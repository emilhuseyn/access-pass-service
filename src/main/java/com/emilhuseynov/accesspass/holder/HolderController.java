package com.emilhuseynov.accesspass.holder;

import com.emilhuseynov.accesspass.common.PageResponse;
import com.emilhuseynov.accesspass.holder.HolderDtos.CreateHolderRequest;
import com.emilhuseynov.accesspass.holder.HolderDtos.HolderResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holders")
class HolderController {

    private final HolderService holderService;

    HolderController(HolderService holderService) {
        this.holderService = holderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HolderResponse create(@Valid @RequestBody CreateHolderRequest request) {
        return HolderResponse.from(holderService.create(request.fullName(), request.email(), request.externalRef()));
    }

    @GetMapping
    PageResponse<HolderResponse> search(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(holderService.search(name, pageable), HolderResponse::from);
    }

    @GetMapping("/{holderId}")
    HolderResponse get(@PathVariable Long holderId) {
        return HolderResponse.from(holderService.get(holderId));
    }

    @PostMapping("/{holderId}/deactivate")
    HolderResponse deactivate(@PathVariable Long holderId) {
        return HolderResponse.from(holderService.deactivate(holderId));
    }

    @PostMapping("/{holderId}/activate")
    HolderResponse activate(@PathVariable Long holderId) {
        return HolderResponse.from(holderService.activate(holderId));
    }
}
