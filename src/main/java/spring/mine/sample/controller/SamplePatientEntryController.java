package spring.mine.sample.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.validator.BaseErrors;
import spring.mine.sample.form.SamplePatientEntryForm;
import spring.mine.sample.validator.SamplePatientEntryFormValidator;
import spring.service.address.OrganizationAddressService;
import spring.service.analysis.AnalysisService;
import spring.service.dataexchange.order.ElectronicOrderService;
import spring.service.observationhistory.ObservationHistoryService;
import spring.service.organization.OrganizationService;
import spring.service.person.PersonService;
import spring.service.provider.ProviderService;
import spring.service.requester.SampleRequesterService;
import spring.service.sample.SampleService;
import spring.service.samplehuman.SampleHumanService;
import spring.service.sampleitem.SampleItemService;
import spring.service.test.TestSectionService;
import spring.service.test.TestService;
import us.mn.state.health.lims.address.valueholder.OrganizationAddress;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.formfields.FormFields;
import us.mn.state.health.lims.common.formfields.FormFields.Field;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.DisplayListService.ListType;
import us.mn.state.health.lims.common.services.SampleAddService.SampleTestCollection;
import us.mn.state.health.lims.common.services.SampleOrderService;
import us.mn.state.health.lims.common.services.StatusService;
import us.mn.state.health.lims.common.services.StatusService.AnalysisStatus;
import us.mn.state.health.lims.common.services.TableIdService;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.common.util.ConfigurationProperties.Property;
import us.mn.state.health.lims.common.util.DateUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.common.util.validator.GenericValidator;
import us.mn.state.health.lims.observationhistory.valueholder.ObservationHistory;
import us.mn.state.health.lims.organization.valueholder.Organization;
import us.mn.state.health.lims.panel.valueholder.Panel;
import us.mn.state.health.lims.patient.action.IPatientUpdate;
import us.mn.state.health.lims.patient.action.IPatientUpdate.PatientUpdateStatus;
import us.mn.state.health.lims.patient.action.bean.PatientManagementInfo;
import us.mn.state.health.lims.patient.action.bean.PatientSearch;
import us.mn.state.health.lims.requester.valueholder.SampleRequester;
import us.mn.state.health.lims.sample.action.util.SamplePatientUpdateData;
import us.mn.state.health.lims.sample.bean.SampleOrderItem;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.test.valueholder.TestSection;

@Controller
public class SamplePatientEntryController extends BaseSampleEntryController {

	@Autowired
	SamplePatientEntryFormValidator formValidator;

	private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";
	@Autowired
	private OrganizationService organizationService;
	@Autowired
	private OrganizationAddressService organizationAddressService;
	@Autowired
	private TestSectionService testSectionService;
	@Autowired
	private ElectronicOrderService electronicOrderService;
	@Autowired
	private ObservationHistoryService observationHistoryService;
	@Autowired
	private PersonService personService;
	@Autowired
	private ProviderService providerService;
	@Autowired
	private SampleService sampleService;
	@Autowired
	private SampleHumanService sampleHumanService;
	@Autowired
	private SampleItemService sampleItemService;
	@Autowired
	private AnalysisService analysisService;
	@Autowired
	private TestService testService;
	@Autowired
	private SampleRequesterService sampleRequesterService;

	@RequestMapping(value = "/SamplePatientEntry", method = RequestMethod.GET)
	public ModelAndView showSamplePatientEntry(HttpServletRequest request)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SamplePatientEntryForm form = new SamplePatientEntryForm();

		request.getSession().setAttribute(SAVE_DISABLED, TRUE);
		SampleOrderService sampleOrderService = new SampleOrderService();
		PropertyUtils.setProperty(form, "sampleOrderItems", sampleOrderService.getSampleOrderItem());
		PropertyUtils.setProperty(form, "patientProperties", new PatientManagementInfo());
		PropertyUtils.setProperty(form, "patientSearch", new PatientSearch());
		PropertyUtils.setProperty(form, "sampleTypes", DisplayListService.getList(ListType.SAMPLE_TYPE_ACTIVE));
		PropertyUtils.setProperty(form, "testSectionList", DisplayListService.getList(ListType.TEST_SECTION));
		PropertyUtils.setProperty(form, "currentDate", DateUtil.getCurrentDateAsText());

		// for (Object program : form.getSampleOrderItems().getProgramList()) {
		// System.out.println(((IdValuePair) program).getValue());
		// }

		addProjectList(form);

		if (FormFields.getInstance().useField(FormFields.Field.InitialSampleCondition)) {
			PropertyUtils.setProperty(form, "initialSampleConditionList",
					DisplayListService.getList(ListType.INITIAL_SAMPLE_CONDITION));
		}

		addFlashMsgsToRequest(request);
		return findForward(FWD_SUCCESS, form);
	}

	@RequestMapping(value = "/SamplePatientEntry", method = RequestMethod.POST)
	public @ResponseBody ModelAndView showSamplePatientEntrySave(HttpServletRequest request,
			@ModelAttribute("form") @Validated(SamplePatientEntryForm.SamplePatientEntry.class) SamplePatientEntryForm form,
			BindingResult result, RedirectAttributes redirectAttributes)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}
		SamplePatientUpdateData updateData = new SamplePatientUpdateData(getSysUserId(request));

		PatientManagementInfo patientInfo = (PatientManagementInfo) PropertyUtils.getProperty(form,
				"patientProperties");
		SampleOrderItem sampleOrder = (SampleOrderItem) PropertyUtils.getProperty(form, "sampleOrderItems");

		boolean trackPayments = ConfigurationProperties.getInstance()
				.isPropertyValueEqual(Property.TRACK_PATIENT_PAYMENT, "true");

		String receivedDateForDisplay = sampleOrder.getReceivedDateForDisplay();

		if (!GenericValidator.isBlankOrNull(sampleOrder.getReceivedTime())) {
			receivedDateForDisplay += " " + sampleOrder.getReceivedTime();
		} else {
			receivedDateForDisplay += " 00:00";
		}

		updateData.setCollectionDateFromRecieveDateIfNeeded(receivedDateForDisplay);
		updateData.initializeRequester(sampleOrder);

		PatientManagementUpdate patientUpdate = new PatientManagementUpdate();
		patientUpdate.setSysUserIdFromRequest(request);
		testAndInitializePatientForSaving(request, patientInfo, patientUpdate, updateData);

		updateData.setAccessionNumber(sampleOrder.getLabNo());
		updateData.initProvider(sampleOrder);
		updateData.initSampleData(form.getSampleXML(), receivedDateForDisplay, trackPayments, sampleOrder);
		updateData.validateSample(result);

		if (result.hasErrors()) {
			saveErrors(result);
			// setSuccessFlag(request, true);
			return findForward(FWD_FAIL_INSERT, form);
		}

		try {
			persistData(updateData, patientUpdate, patientInfo, form);
		} catch (LIMSRuntimeException lre) {
			// ActionError error;
			if (lre.getException() instanceof StaleObjectStateException) {
				// error = new ActionError("errors.OptimisticLockException", null, null);
				result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
			} else {
				lre.printStackTrace();
				// error = new ActionError("errors.UpdateException", null, null);
				result.reject("errors.UpdateException", "errors.UpdateException");
			}
			System.out.println(result);

			// errors.add(ActionMessages.GLOBAL_MESSAGE, error);
			saveErrors(result);
			request.setAttribute(ALLOW_EDITS_KEY, "false");
			return findForward(FWD_FAIL_INSERT, form);

		}

		redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
		return findForward(FWD_SUCCESS_INSERT, form);
	}

	@Transactional
	public void persistData(SamplePatientUpdateData updateData, PatientManagementUpdate patientUpdate,
			PatientManagementInfo patientInfo, SamplePatientEntryForm form) {
		boolean useInitialSampleCondition = FormFields.getInstance().useField(Field.InitialSampleCondition);

		persistOrganizationData(updateData);

		if (updateData.isSavePatient()) {
			patientUpdate.persistPatientData(patientInfo);
		}

		updateData.setPatientId(patientUpdate.getPatientId(form));

		persistProviderData(updateData);
		persistSampleData(updateData);
		persistRequesterData(updateData);
		if (useInitialSampleCondition) {
			persistInitialSampleConditions(updateData);
		}

		persistObservations(updateData);

		request.getSession().setAttribute("lastAccessionNumber", updateData.getAccessionNumber());
		request.getSession().setAttribute("lastPatientId", updateData.getPatientId());
	}

	private void persistObservations(SamplePatientUpdateData updateData) {

		for (ObservationHistory observation : updateData.getObservations()) {
			observation.setSampleId(updateData.getSample().getId());
			observation.setPatientId(updateData.getPatientId());
			observationHistoryService.insert(observation);
		}
	}

	private void testAndInitializePatientForSaving(HttpServletRequest request, PatientManagementInfo patientInfo,
			IPatientUpdate patientUpdate, SamplePatientUpdateData updateData)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		patientUpdate.setPatientUpdateStatus(patientInfo);
		updateData.setSavePatient(patientUpdate.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION);

		if (updateData.isSavePatient()) {
			updateData.setPatientErrors(patientUpdate.preparePatientData(request, patientInfo));
		} else {
			updateData.setPatientErrors(new BaseErrors());
		}
	}

	private void persistOrganizationData(SamplePatientUpdateData updateData) {
		Organization newOrganization = updateData.getNewOrganization();
		if (newOrganization != null) {
			organizationService.insert(newOrganization);
			organizationService.linkOrganizationAndType(newOrganization, TableIdService.REFERRING_ORG_TYPE_ID);
			if (updateData.getRequesterSite() != null) {
				updateData.getRequesterSite().setRequesterId(newOrganization.getId());
			}

			for (OrganizationAddress address : updateData.getOrgAddressExtra()) {
				address.setOrganizationId(newOrganization.getId());
				organizationAddressService.insert(address);
			}
		}

		if (updateData.getCurrentOrganization() != null) {
			organizationService.update(updateData.getCurrentOrganization());
		}

	}

	private void persistProviderData(SamplePatientUpdateData updateData) {
		if (updateData.getProviderPerson() != null && updateData.getProvider() != null) {

			personService.insert(updateData.getProviderPerson());
			updateData.getProvider().setPerson(updateData.getProviderPerson());

			providerService.insert(updateData.getProvider());
		}
	}

	private void persistSampleData(SamplePatientUpdateData updateData) {
		String analysisRevision = SystemConfiguration.getInstance().getAnalysisDefaultRevision();

		sampleService.insertDataWithAccessionNumber(updateData.getSample());

		// if (!GenericValidator.isBlankOrNull(projectId)) {
		// persistSampleProject();
		// }

		for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {

			sampleItemService.insert(sampleTestCollection.item);

			for (Test test : sampleTestCollection.tests) {
				test = testService.get(test.getId());

				Analysis analysis = populateAnalysis(analysisRevision, sampleTestCollection, test,
						sampleTestCollection.testIdToUserSectionMap.get(test.getId()),
						sampleTestCollection.testIdToUserSampleTypeMap.get(test.getId()), updateData);
				analysisService.insert(analysis, false); // false--do not check for duplicates
			}

		}

		updateData.buildSampleHuman();

		sampleHumanService.insert(updateData.getSampleHuman());

		if (updateData.getElectronicOrder() != null) {
			electronicOrderService.update(updateData.getElectronicOrder());
		}
	}

	/*
	 * private void persistSampleProject() throws LIMSRuntimeException {
	 * SampleProjectDAO sampleProjectDAO = new SampleProjectDAOImpl(); ProjectDAO
	 * projectDAO = new ProjectDAOImpl(); Project project = new Project(); //
	 * project.setId(projectId); projectDAO.getData(project);
	 *
	 * SampleProject sampleProject = new SampleProject();
	 * sampleProject.setProject(project); sampleProject.setSample(sample);
	 * sampleProject.setSysUserId(getSysUserId(request));
	 * sampleProjectDAO.insertData(sampleProject); }
	 */

	private void persistRequesterData(SamplePatientUpdateData updateData) {
		if (updateData.getProviderPerson() != null
				&& !GenericValidator.isBlankOrNull(updateData.getProviderPerson().getId())) {
			SampleRequester sampleRequester = new SampleRequester();
			sampleRequester.setRequesterId(updateData.getProviderPerson().getId());
			sampleRequester.setRequesterTypeId(TableIdService.PROVIDER_REQUESTER_TYPE_ID);
			sampleRequester.setSampleId(Long.parseLong(updateData.getSample().getId()));
			sampleRequester.setSysUserId(updateData.getCurrentUserId());
			sampleRequesterService.insert(sampleRequester);
		}

		if (updateData.getRequesterSite() != null) {
			updateData.getRequesterSite().setSampleId(Long.parseLong(updateData.getSample().getId()));
			if (updateData.getNewOrganization() != null) {
				updateData.getRequesterSite().setRequesterId(updateData.getNewOrganization().getId());
			}
			sampleRequesterService.insert(updateData.getRequesterSite());
		}
	}

	private void persistInitialSampleConditions(SamplePatientUpdateData updateData) {

		for (SampleTestCollection sampleTestCollection : updateData.getSampleItemsTests()) {
			List<ObservationHistory> initialConditions = sampleTestCollection.initialSampleConditionIdList;

			if (initialConditions != null) {
				for (ObservationHistory observation : initialConditions) {
					observation.setSampleId(sampleTestCollection.item.getSample().getId());
					observation.setSampleItemId(sampleTestCollection.item.getId());
					observation.setPatientId(updateData.getPatientId());
					observation.setSysUserId(updateData.getCurrentUserId());
					observationHistoryService.insert(observation);
				}
			}
		}
	}

	private Analysis populateAnalysis(String analysisRevision, SampleTestCollection sampleTestCollection, Test test,
			String userSelectedTestSection, String sampleTypeName, SamplePatientUpdateData updateData) {
		java.sql.Date collectionDateTime = DateUtil.convertStringDateTimeToSqlDate(sampleTestCollection.collectionDate);
		TestSection testSection = test.getTestSection();
		if (!GenericValidator.isBlankOrNull(userSelectedTestSection)) {
			testSection = testSectionService.get(userSelectedTestSection);
		}

		Panel panel = updateData.getSampleAddService().getPanelForTest(test);

		Analysis analysis = new Analysis();
		analysis.setTest(test);
		analysis.setPanel(panel);
		analysis.setIsReportable(test.getIsReportable());
		analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
		analysis.setSampleItem(sampleTestCollection.item);
		analysis.setSysUserId(sampleTestCollection.item.getSysUserId());
		analysis.setRevision(analysisRevision);
		analysis.setStartedDate(collectionDateTime == null ? DateUtil.getNowAsSqlDate() : collectionDateTime);
		analysis.setStatusId(StatusService.getInstance().getStatusID(AnalysisStatus.NotStarted));
		if (!GenericValidator.isBlankOrNull(sampleTypeName)) {
			analysis.setSampleTypeName(sampleTypeName);
		}
		analysis.setTestSection(testSection);
		return analysis;
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "samplePatientEntryDefinition";
		} else if (FWD_FAIL.equals(forward)) {
			return "homePageDefinition";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "redirect:/SamplePatientEntry.do";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "samplePatientEntryDefinition";
		} else {
			return "PageNotFound";
		}
	}
}
