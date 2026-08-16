package SmartHarvest360.api;

import SmartHarvest360.api.dto.BuyersResponse;
import SmartHarvest360.api.dto.SellRequest;
import SmartHarvest360.api.dto.SellResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Market comparison and sale endpoints (wrap HarvestMarketController logic). */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final SessionService service;

    public MarketController(SessionService service) {
        this.service = service;
    }

    /** Buyer comparison (prices, best market, price history) for every ready, unsold crop. */
    @GetMapping("/buyers")
    public BuyersResponse buyers() {
        return service.buyers();
    }

    /** Records a sale at the best current price and persists it (CSV + optional MySQL). */
    @PostMapping("/sell")
    public SellResponse sell(@RequestBody SellRequest request) {
        return service.sell(request);
    }
}
