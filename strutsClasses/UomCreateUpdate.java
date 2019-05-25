package us.mn.state.health.lims.testconfiguration.action;

	import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Transaction;

import us.mn.state.health.lims.common.action.BaseAction;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.localization.valueholder.Localization;
import us.mn.state.health.lims.role.dao.RoleDAO;
import us.mn.state.health.lims.role.daoimpl.RoleDAOImpl;
import us.mn.state.health.lims.role.valueholder.Role;
import us.mn.state.health.lims.systemmodule.dao.SystemModuleDAO;
import us.mn.state.health.lims.systemmodule.daoimpl.SystemModuleDAOImpl;
import us.mn.state.health.lims.systemmodule.valueholder.SystemModule;
import us.mn.state.health.lims.systemusermodule.daoimpl.RoleModuleDAOImpl;
import us.mn.state.health.lims.systemusermodule.valueholder.RoleModule;
import us.mn.state.health.lims.unitofmeasure.daoimpl.UnitOfMeasureDAOImpl;
import us.mn.state.health.lims.unitofmeasure.valueholder.UnitOfMeasure;

	public class UomCreateUpdate extends BaseAction {
	    @Override
	    protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
	        RoleDAO roleDAO = new RoleDAOImpl();
	        RoleModuleDAOImpl roleModuleDAO = new RoleModuleDAOImpl();
	        SystemModuleDAO systemModuleDAO = new SystemModuleDAOImpl();
	        DynaValidatorForm dynaForm = (DynaValidatorForm)form;
	        String identifyingName = dynaForm.getString("uomEnglishName");
	        String userId = getSysUserId(request);

	        //Localization localization = createLocalization(dynaForm.getString("uomFrenchName"), identifyingName, userId);

	        UnitOfMeasure unitOfMeasure = createUnitOfMeasure(identifyingName, userId);

	        SystemModule workplanModule = createSystemModule("Workplan", identifyingName, userId);
	        SystemModule resultModule = createSystemModule("LogbookResults", identifyingName, userId);
	        SystemModule validationModule = createSystemModule("ResultValidation", identifyingName, userId);

	        Role resultsEntryRole = roleDAO.getRoleByName("Results entry");
	        Role validationRole = roleDAO.getRoleByName("Validator");

	        RoleModule workplanResultModule = createRoleModule(userId, workplanModule, resultsEntryRole);
	        RoleModule resultResultModule = createRoleModule(userId, resultModule, resultsEntryRole);
	        RoleModule validationValidationModule = createRoleModule(userId, validationModule, validationRole);

	        Transaction tx = HibernateUtil.getSession().beginTransaction();

	        try {
	            //new LocalizationDAOImpl().insert(localization);
	            //unitOfMeasure.setLocalization(localization);
	            new UnitOfMeasureDAOImpl().insertData(unitOfMeasure);
	            //systemModuleDAO.insertData(workplanModule);
	            //systemModuleDAO.insertData(resultModule);
	            //systemModuleDAO.insertData(validationModule);
	            //roleModuleDAO.insertData(workplanResultModule);
	            //roleModuleDAO.insertData(resultResultModule);
	            //roleModuleDAO.insertData(validationValidationModule);

	            tx.commit();

	        } catch (LIMSRuntimeException lre) {
	            tx.rollback();
	            lre.printStackTrace();
	        } finally {
	            HibernateUtil.closeSession();
	        }

	        DisplayListService.getInstance().refreshList(DisplayListService.ListType.UNIT_OF_MEASURE);
	        DisplayListService.getInstance().refreshList(DisplayListService.ListType.UNIT_OF_MEASURE_INACTIVE);

	        return mapping.findForward(FWD_SUCCESS);
	    }

	    private Localization createLocalization( String french, String english, String currentUserId) {
	        Localization localization = new Localization();
	        localization.setEnglish(english);
	        localization.setFrench(french);
	        localization.setDescription("uom name");
	        localization.setSysUserId(currentUserId);
	        return localization;
	    }

	    private RoleModule createRoleModule(String userId, SystemModule workplanModule, Role role) {
	        RoleModule roleModule = new RoleModule();
	        roleModule.setRole(role);
	        roleModule.setSystemModule(workplanModule);
	        roleModule.setSysUserId(userId);
	        roleModule.setHasAdd("Y");
	        roleModule.setHasDelete("Y");
	        roleModule.setHasSelect("Y");
	        roleModule.setHasUpdate("Y");
	        return roleModule;
	    }

	    private UnitOfMeasure createUnitOfMeasure(String identifyingName, String userId) {
	        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
	        unitOfMeasure.setDescription(identifyingName);
	        unitOfMeasure.setUnitOfMeasureName(identifyingName);
	        return unitOfMeasure;
	    }

	    private SystemModule createSystemModule(String menuItem, String identifyingName, String userId) {
	        SystemModule module = new SystemModule();
	        module.setSystemModuleName(menuItem + ":" + identifyingName);
	        module.setDescription(menuItem + "=>" + identifyingName);
	        module.setSysUserId(userId);
	        module.setHasAddFlag("Y");
	        module.setHasDeleteFlag("Y");
	        module.setHasSelectFlag("Y");
	        module.setHasUpdateFlag("Y");
	        return module;
	    }


	    @Override
	    protected String getPageTitleKey() {
	        return null;
	    }

	    @Override
	    protected String getPageSubtitleKey() {
	        return null;
	    }
	}

