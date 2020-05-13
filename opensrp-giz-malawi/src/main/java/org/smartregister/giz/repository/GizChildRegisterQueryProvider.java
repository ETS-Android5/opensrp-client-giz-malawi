package org.smartregister.giz.repository;

import org.smartregister.child.provider.RegisterQueryProvider;
import org.smartregister.child.util.Constants;
import org.smartregister.giz.util.GizConstants;
import org.smartregister.growthmonitoring.util.GrowthMonitoringConstants;

public class GizChildRegisterQueryProvider extends RegisterQueryProvider {

    @Override
    public String[] mainColumns() {
        return new String[]{
                getDemographicTable() + "." + Constants.KEY.ID + " as _id",
                getDemographicTable() + "." + Constants.KEY.RELATIONALID,
                getDemographicTable() + "." + Constants.KEY.ZEIR_ID,
                getChildDetailsTable() + "." + Constants.KEY.RELATIONAL_ID,
                getDemographicTable() + "." + Constants.KEY.GENDER,
                getDemographicTable() + "." + Constants.KEY.BASE_ENTITY_ID,
                getDemographicTable() + "." + Constants.KEY.FIRST_NAME,
                getDemographicTable() + "." + Constants.KEY.LAST_NAME,
                "mother" + "." + Constants.KEY.FIRST_NAME + " as mother_first_name",
                "mother" + "." + Constants.KEY.LAST_NAME + " as mother_last_name",
                getDemographicTable() + "." + Constants.KEY.DOB,
                "mother" + "." + Constants.KEY.DOB + " as mother_dob",
                getMotherDetailsTable() + "." + Constants.KEY.NRC_NUMBER + " as mother_nrc_number",
                getDemographicTable() + "." + Constants.KEY.CLIENT_REG_DATE,
                getDemographicTable() + "." + Constants.KEY.LAST_INTERACTED_WITH,
                getChildDetailsTable() + "." + Constants.KEY.INACTIVE,
                getChildDetailsTable() + "." + Constants.KEY.LOST_TO_FOLLOW_UP,
                getDemographicTable() + "." + Constants.KEY.VILLAGE,
                getDemographicTable() + "." + Constants.KEY.HOME_ADDRESS,
                getChildDetailsTable() + "." + Constants.SHOW_BCG_SCAR,
                getChildDetailsTable() + "." + Constants.SHOW_BCG2_REMINDER,
                getMotherDetailsTable() + "." + GizConstants.PROTECTED_AT_BIRTH,
                getMotherDetailsTable() + "." + GizConstants.MOTHER_TDV_DOSES,
                getMotherDetailsTable() + "." + GizConstants.MOTHER_HIV_STATUS,
                getChildDetailsTable() + "." + GizConstants.BIRTH_REGISTRATION_NUMBER,
                getChildDetailsTable() + "." + GrowthMonitoringConstants.PMTCT_STATUS
        };
    }

}
