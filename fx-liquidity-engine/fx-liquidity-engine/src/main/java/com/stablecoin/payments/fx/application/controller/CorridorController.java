package com.stablecoin.payments.fx.application.controller;

import com.stablecoin.payments.fx.api.response.CorridorResponse;
import com.stablecoin.payments.fx.application.mapper.FxResponseMapper;
import com.stablecoin.payments.fx.domain.service.LiquidityPoolQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/fx")
@RequiredArgsConstructor
public class CorridorController {

    private final LiquidityPoolQueryHandler queryHandler;
    private final FxResponseMapper responseMapper;

    @GetMapping("/corridors")
    public List<CorridorResponse> listCorridors() {
        log.info("GET /v1/fx/corridors");
        return queryHandler.listCorridors().stream()
                .map(responseMapper::toResponse)
                .toList();
    }
}
