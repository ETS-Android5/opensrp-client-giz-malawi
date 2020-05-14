package org.smartregister.giz.configuration;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.maternity.configuration.MaternityRegisterQueryProviderContract;

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 2020-03-31
 */

public class GizMaternityRegisterQueryProvider extends MaternityRegisterQueryProviderContract {

    @NonNull
    @Override
    public String getObjectIdsQuery(@Nullable String filters, @Nullable String mainCondition) {
        if (TextUtils.isEmpty(filters)) {
            return "SELECT object_id, last_interacted_with FROM ec_client_search INNER JOIN client_register_type ON ec_client_search.object_id = client_register_type.base_entity_id " +
                    "WHERE client_register_type.register_type = 'maternity' ORDER BY ec_client_search.last_interacted_with DESC";
        } else {
            String sql = "SELECT object_id FROM ec_client_search INNER JOIN client_register_type ON ec_client_search.object_id = client_register_type.base_entity_id " +
                    "WHERE client_register_type.register_type = 'maternity' AND ec_client_search.date_removed IS NULL AND ec_client_search.phrase MATCH '%s*'" +
                    "ORDER BY ec_client_search.last_interacted_with DESC";
            sql = sql.replace("%s", filters);
            return sql;
        }
    }

    @NonNull
    @Override
    public String[] countExecuteQueries(@Nullable String filters, @Nullable String mainCondition) {
        SmartRegisterQueryBuilder sqb = new SmartRegisterQueryBuilder();
        return new String[] {
                sqb.countQueryFts("ec_client", null, "register_type = 'maternity'", filters)
        };
    }

    @NonNull
    @Override
    public String mainSelectWhereIDsIn() {
        return "SELECT ec_client.id AS _id , ec_client.first_name , ec_client.last_name , '' AS middle_name , ec_client.gender , ec_client.dob , '' AS home_address, maternity_details.conception_date, maternity_details.hiv_status_current, ec_client.relationalid , ec_client.opensrp_id AS register_id , ec_client.last_interacted_with, 'ec_client' as entity_table FROM ec_client INNER JOIN maternity_registration_details maternity_details ON ec_client.base_entity_id = maternity_details.base_entity_id " +
                " INNER JOIN client_register_type ON ec_client.base_entity_id = client_register_type.base_entity_id WHERE client_register_type.register_type = 'maternity' AND ec_client.id IN (%s) " +
                "ORDER BY ec_client.last_interacted_with DESC";
    }
}