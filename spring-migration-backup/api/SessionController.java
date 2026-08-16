package SmartHarvest360.api;

import SmartHarvest360.api.dto.SessionStartRequest;
import SmartHarvest360.api.dto.SessionStartResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session lifecycle endpoints. */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    /** Initializes the shared farm session and returns its id ("1"). */
    @PostMapping("/start")
    public SessionStartResponse start(@RequestBody(required = false) SessionStartRequest request) {
        return service.startSession(request == null ? null : request.farmerName());
    }
}
