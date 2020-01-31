
import axg.shuffle.form.util.FormInfoMap;
import axg.shuffle.form.util.FormalizeUtils;
import exception.AISTProcessException;
import java.util.HashMap;
import java.util.Map;
import mongodb.MongoDBPOJOData;
import obj.MSyaryoObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ZZ17807
 */
public class CreateInfoDB {
    public static void main(String[] args) throws AISTProcessException {
        Map cmap = new HashMap();
        cmap.put("MAX_LEAST_DATE", "20190521");
        FormInfoMap map = new FormInfoMap("json.KM_PC200_DB", cmap);
        
        cmap.put("MACHINE_TYPE", "20190521");
        
        FormalizeUtils.createFormInfo(map);
        
        System.out.println(FormalizeUtils.getFormInfo("json.KM_PC200_DB"));
    }
}
