package org.paykesho.backoffice.settings.controller;

import org.commons.dto.StateDto;
import org.commons.entity.State;
import org.commons.entity.settings.Country;
import org.paykesho.backoffice.settings.repository.StateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/states")
public class StateRestController {

    @Autowired
    private StateRepository stateRepository;

    @GetMapping("/list-by-country/{uuid}")
    public List<StateDto> listByCountry(@PathVariable("uuid") String countryUuid) {
        // For settings/location data, using UUID is optional but recommended for consistency
        // This endpoint is typically internal, but we'll support UUID for security consistency
        List<State> stateList = stateRepository.findByCountryUuid(countryUuid);
        List<StateDto> result = new ArrayList<>();

        for (State state : stateList) {
            result.add(new StateDto(state.getId(), state.getName()));
        }

        return result;
    }

    @PostMapping("/save")
    public String save(@RequestBody State state) {
        State savedState = stateRepository.save(state);
        return String.valueOf(savedState.getId());
    }

    @GetMapping("/delete/{uuid}")
    public void delete(@PathVariable("uuid") String uuid) {
        State state = stateRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("State not found with UUID: " + uuid));
        stateRepository.deleteById(state.getId());
    }

}
