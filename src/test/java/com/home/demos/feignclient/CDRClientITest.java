package com.home.demos.feignclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.home.demos.feignclient.config.FhirClientConfig;
import com.home.demos.feignclient.testclients.ESSCDRClient;
import com.home.demos.feignclient.utils.ResourceUtils;
import com.home.demos.feignclient.utils.WireMockInitializer;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {CDRClientRegistrar.class, FhirClientConfig.class})
@ContextConfiguration(initializers = WireMockInitializer.class)
class CDRClientITest {

    private static final String GET_METADATA_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-metadata-success.json";
    private static final String GET_ENCOUNTER_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-encounter-success.json";
    private static final String GET_DOCUMENT_REFERENCES_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-document-references-success.json";
    private static final String GET_CHARGE_ITEMS_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-chargeitem-success.json";
    private static final String POST_COMMUNICATION_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-post-communication-success.json";
    private static final String GET_MEDICATION_REQUESTS_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-medication-requests-success.json";
    private static final String GET_QUESTIONNAIRE_RESPONSES_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-questionnaire-responses-success.json";
    private static final String GET_PRACTITIONER_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-practitioner-success.json";
    private static final String GET_PRACTITIONER_ROLE_SUCCESS_RESPONSE_PATH = "stubs/responses/cdr-get-practitioner-role-success.json";

    private static final String ENCOUNTER_ID = "80c23dfe4a53";
    private static final String PROVIDER_ID = "4e18263d-7f40-4b4c-a274-264d8357fc9e";
    private static final String PATIENT_ID = "d17014ea-ad8d-4bb8-9924-49fb227f9473";
    private static final String DOCUMENT_REFERENCE_ID = "749f2e14-c370-4b48-8197-6e72c85f36bb";
    private static final List<String> EXPECTED_CHARGE_ITEMS = List.of(
            "4d4a524d-31e3-4431-b696-fdd0ec2f110d",
            "8ccab534-0da4-4376-a546-2e4d615fa01f",
            "6cede1f9-8594-44dd-8e60-46e5c33afdd0"
    );
    private static final List<String> EXPECTED_MEDICATION_REQUEST_ITEMS = List.of(
            "40b442e6-cc50-4126-a11c-8899b6b23e2b",
            "cac52fad-3bf5-425d-bf77-b2ae29d57bed",
            "5e2ba625-0329-4ed0-8197-515215d6ffef",
            "c06f5b42-1e7d-4467-a22f-4ddd22746d0f",
            "a54f568f-589c-467f-932a-d7c9a23a06f7"
    );
    private static final String EXPECTED_QUESTIONNAIRE_RESPONSE_ID = "77c1bca6-8009-4ea3-8a86-c41bcbdc23a4";
    private static final String PRACTITIONER_ID = "4e18263d-7f40-4b4c-a274-264d8357fc9e";
    private static final String PRACTITIONER_ROLE_ID = "6f4f303d-97b1-4101-bd24-ff3aa0fd82f4";
    private static final String COMMUNICATION_ID = "4e18263d-7f40-4b4c-a274-264d8357fc9e";
    private static final String CURRENT_STATUS = "Current";
    private static final String REFERRALS_TYPE = "57133-1";

    @Autowired
    private ESSCDRClient esscdrClient;

    @Autowired
    private WireMockServer server;

    @BeforeEach
    void setUp() {
        var metadata = ResourceUtils.readFromClasspath(GET_METADATA_SUCCESS_RESPONSE_PATH);
        server.stubFor(
                WireMock.get("/metadata").willReturn(WireMock.okJson(metadata))
        );
    }

    @AfterEach
    void tearDown() {
        server.resetAll();
    }

    @Test
    void shouldFindEncounterById() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_ENCOUNTER_SUCCESS_RESPONSE_PATH);
        server.stubFor(
                WireMock.get("/Encounter?_id=" + ENCOUNTER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var encounter = esscdrClient.findOneEncounterById(ENCOUNTER_ID);

        //then
        assertThat(encounter)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(ENCOUNTER_ID);
    }

    @Test
    void shouldFindAllDocumentReferencesByAuthorAndSubject() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_DOCUMENT_REFERENCES_SUCCESS_RESPONSE_PATH);
        server.stubFor(
                WireMock.get("/DocumentReference?author=" + PROVIDER_ID + "&subject=" + PATIENT_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var documentReferences = esscdrClient.findAllDocumentReferencesByAuthorAndSubject(PROVIDER_ID, PATIENT_ID);

        //then
        assertThat(documentReferences)
                .hasSize(1)
                .first()
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(DOCUMENT_REFERENCE_ID);
    }

    @Test
    void shouldFindAllChargeItemsByContext() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_CHARGE_ITEMS_SUCCESS_RESPONSE_PATH);
        server.stubFor(
                WireMock.get("/ChargeItem?context=" + ENCOUNTER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var chargeItems = esscdrClient.findAllChargeItemsByContext(ENCOUNTER_ID);

        //then
        assertThat(chargeItems)
                .hasSize(3)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .containsAll(EXPECTED_CHARGE_ITEMS);
    }

    @Test
    void shouldSaveNewCommunication() {
        //given
        var data = ResourceUtils.readFromClasspath(POST_COMMUNICATION_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.post("/Communication").willReturn(WireMock.okJson(data))
        );

        //when
        var communication = new Communication();
        communication.setStatus(Communication.CommunicationStatus.PREPARATION);

        var savedCommunication = esscdrClient.save(communication);

        //then
        assertThat(savedCommunication)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(ENCOUNTER_ID);
    }

    @Test
    void shouldUpdateCommunication() {
        //given
        var data = ResourceUtils.readFromClasspath(POST_COMMUNICATION_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.put("/Communication/" + COMMUNICATION_ID).willReturn(WireMock.okJson(data))
        );

        //when
        var communication = new Communication();
        communication.setId(COMMUNICATION_ID);
        communication.setStatus(Communication.CommunicationStatus.ONHOLD);

        var savedCommunication = esscdrClient.update(communication);

        //then
        assertThat(savedCommunication)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(COMMUNICATION_ID);
    }

    @Test
    void shouldFindAllMedicationRequestsByEncounter() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_MEDICATION_REQUESTS_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.get("/MedicationRequest?encounter=" + ENCOUNTER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var medicationRequests = esscdrClient.findAllMedicationRequestsByEncounter(ENCOUNTER_ID);

        //then
        assertThat(medicationRequests)
                .hasSize(5)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .containsAll(EXPECTED_MEDICATION_REQUEST_ITEMS);
    }

    @Test
    void shouldFindAllQuestionnaireResponsesByEncounter() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_QUESTIONNAIRE_RESPONSES_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.get("/QuestionnaireResponse?encounter=" + ENCOUNTER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var questionnaireResponses = esscdrClient.findAllQuestionnaireResponsesByEncounter(ENCOUNTER_ID);

        //then
        assertThat(questionnaireResponses)
                .hasSize(1)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .contains(EXPECTED_QUESTIONNAIRE_RESPONSE_ID);
    }

    @Test
    void shouldFindOnePractitionerById() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_PRACTITIONER_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.get("/Practitioner?_id=" + PRACTITIONER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var practitioner = esscdrClient.findOnePractitionerById(PRACTITIONER_ID);

        //then
        assertThat(practitioner)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(PRACTITIONER_ID);
    }

    @Test
    void shouldFindOnePractitionerRoleByPractitioner() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_PRACTITIONER_ROLE_SUCCESS_RESPONSE_PATH);

        server.stubFor(
                WireMock.get("/PractitionerRole?practitioner=" + PRACTITIONER_ID + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var practitionerRole = esscdrClient.findOnePractitionerRoleByPractitioner(PRACTITIONER_ID);

        //then
        assertThat(practitionerRole)
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(PRACTITIONER_ROLE_ID);
    }

    @Test
    void shouldFindAllDocumentReferencesByEncounterAndStatusAndType() {
        //given
        var data = ResourceUtils.readFromClasspath(GET_DOCUMENT_REFERENCES_SUCCESS_RESPONSE_PATH);
        server.stubFor(
                WireMock.get("/DocumentReference?encounter=" + ENCOUNTER_ID + "&status=" + CURRENT_STATUS + "&type=" + REFERRALS_TYPE + "&_include=*").willReturn(WireMock.okJson(data))
        );

        //when
        var documentReferences = esscdrClient.findAllDocumentReferencesByEncounterAndStatusAndType(ENCOUNTER_ID, CURRENT_STATUS, REFERRALS_TYPE);

        //then
        assertThat(documentReferences)
                .hasSize(1)
                .first()
                .extracting(Resource::getIdElement)
                .extracting(IdType::getIdPart)
                .isEqualTo(DOCUMENT_REFERENCE_ID);
    }
}