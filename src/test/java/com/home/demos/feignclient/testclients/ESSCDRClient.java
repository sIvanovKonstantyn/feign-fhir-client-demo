package com.home.demos.feignclient.testclients;

import com.home.demos.feignclient.CDRClient;
import org.hl7.fhir.r4.model.*;

import java.util.List;

@CDRClient
public interface ESSCDRClient {
    Encounter findOneEncounterById(String id);

    List<DocumentReference> findAllDocumentReferencesByAuthorAndSubject(String author, String subject);

    List<ChargeItem> findAllChargeItemsByContext(String context);

    Communication save(Communication communication);

    Communication update(Communication communication);

    List<MedicationRequest> findAllMedicationRequestsByEncounter(String encounterId);

    List<QuestionnaireResponse> findAllQuestionnaireResponsesByEncounter(String encounterId);

    Practitioner findOnePractitionerById(String id);

    PractitionerRole findOnePractitionerRoleByPractitioner(String practitionerId);

    List<DocumentReference> findAllDocumentReferencesByEncounterAndStatusAndType(String encounterId, String status, String type);
}
