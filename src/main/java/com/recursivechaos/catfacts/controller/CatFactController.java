/**
 * Created by Andrew Bell 12/7/2015
 * www.recursivechaos.com
 * andrew@recursivechaos.com
 * Licensed under MIT License 2015. See license.txt for details.
 */

package com.recursivechaos.catfacts.controller;

import com.recursivechaos.catfacts.domain.CatFact;
import com.recursivechaos.catfacts.repository.CatFactRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

@RestController
@RequestMapping("/catfacts")
public class CatFactController {

    private static final Logger logger = getLogger(CatFactController.class);
    public static final String MODERATION_MESSAGE = "Your cat fact has been submitted and is waiting moderation. Please check back later using the value from the location header.";

    @Autowired
    CatFactRepository catFactRepository;

    @RequestMapping("/random")
    public ResponseEntity<CatFact> getRandomCatFact() {
        // Todo: Performance concerns. Need to query a single random fact from DB instead of pulling whole list
        List<CatFact> moderatedFacts = catFactRepository.findByModeratedTrue();
        CatFact catFact = getRandomFact(moderatedFacts);
        logger.debug("Getting random cat fact:  {}", catFact);
        HttpHeaders headers = createHeaders(catFact);
        return new ResponseEntity<>(catFact, headers, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> createCatFact(@RequestBody CatFact catFact) {
        logger.debug("Received POST request: {}", catFact);
        catFact.setModerated(false);
        catFact = catFactRepository.save(catFact);
        logger.debug("Saved catfact: {}", catFact);
        HttpHeaders headers = createHeaders(catFact);
        return new ResponseEntity<>(MODERATION_MESSAGE, headers, HttpStatus.ACCEPTED);
    }

    @RequestMapping("/{id}")
    public ResponseEntity<CatFact> getCatFact(@PathVariable("id") Long id, HttpServletRequest request) {
        CatFact catFact = catFactRepository.findOne(id);
        if (null == catFact) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (!catFact.isModerated() && (null == request.getUserPrincipal() || !request.isUserInRole("ROLE_ADMIN"))) {
            throw new BadCredentialsException("CatFact is awaiting moderation.");
        } else {
            return new ResponseEntity<>(catFact, HttpStatus.OK);
        }
    }

    private HttpHeaders createHeaders(CatFact catFact) {
        HttpHeaders httpHeaders = new HttpHeaders();
        // TODO: Extract external URI, or figure out how to assume
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromUriString("http://localhost:8080/catfacts").path("/{id}").buildAndExpand(catFact.getId()).toUri());
        return httpHeaders;
    }

    private static CatFact getRandomFact(List<CatFact> facts) {
        int rand = ThreadLocalRandom.current().nextInt(0, facts.size());
        return facts.get(rand);
    }
}
