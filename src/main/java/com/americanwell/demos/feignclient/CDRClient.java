package com.americanwell.demos.feignclient;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/*
* Indicates an interface as CDR client.
* All methods specified in this interface will be support by dynamic proxy
*
* Possible methods naming patterns:
* {@code findOne[ResourceName]By[FieldName]And[FieldName]}: retrieves *one* object by input parameters.
* Example: PractitionerRole findOnePractitionerRoleByPractitioner(String practitionerId)
*
* {@code findAll[ResourceName]By[FieldName]And[FieldName]}: retrieves *one* object by input parameters.
* Example: List<MedicationRequest> findAllMedicationRequestsByEncounter(String encounterId)
*
* {@code save}: saves object from input parameter.
* Example: Communication save(Communication communication)
*
* {@code update}: updates object from input parameter.
* Example: Communication update(Communication communication)
* */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface CDRClient {
}
