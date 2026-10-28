package com.emilhuseynov.accesspass.pass;

import com.emilhuseynov.accesspass.common.PageResponse;
import com.emilhuseynov.accesspass.pass.PassDtos.IssuePassRequest;
import com.emilhuseynov.accesspass.pass.PassDtos.IssuedPassResponse;
import com.emilhuseynov.accesspass.pass.PassDtos.PassResponse;
import com.emilhuseynov.accesspass.pass.PassDtos.RevokePassRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passes")
class PassController {

    private final PassService passService;
    private final QrCodeRenderer qrCodeRenderer;

    PassController(PassService passService, QrCodeRenderer qrCodeRenderer) {
        this.passService = passService;
        this.qrCodeRenderer = qrCodeRenderer;
    }

    @PostMapping
    ResponseEntity<IssuedPassResponse> issue(@Valid @RequestBody IssuePassRequest request, Principal principal) {
        IssuedPass issued = passService.issue(request.holderId(), request.policyId(), request.validFrom(),
                request.validUntil(), principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/passes/" + issued.pass().getId()))
                .cacheControl(CacheControl.noStore())
                .body(IssuedPassResponse.from(issued));
    }

    @GetMapping("/{passId}")
    PassResponse get(@PathVariable UUID passId) {
        return PassResponse.from(passService.get(passId));
    }

    @GetMapping
    PageResponse<PassResponse> listForHolder(
            @RequestParam Long holderId,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(passService.listForHolder(holderId, pageable), PassResponse::from);
    }

    @PostMapping("/{passId}/revoke")
    PassResponse revoke(@PathVariable UUID passId, @Valid @RequestBody RevokePassRequest request,
                        Principal principal) {
        return PassResponse.from(passService.revoke(passId, request.reason(), principal.getName()));
    }

    @GetMapping(value = "/{passId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> qrCode(@PathVariable UUID passId,
                                  @RequestParam(defaultValue = "320") @Min(128) @Max(1024) int size) {
        byte[] png = qrCodeRenderer.renderPng(passService.currentToken(passId), size);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(png);
    }
}
