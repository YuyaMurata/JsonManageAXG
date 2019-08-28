/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package axg.form.rule;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zz17390
 */
public class DataRejectRule {
    String newd;
    public String getNew(){
        return newd;
    }
    
    public void addNew(String nd){
        this.newd = nd.split("#")[0];
    }
    
    List work = new ArrayList();
    public List getWORKID(){
        return work;
    }
    
    public void addWORKID(String id){
        work.add(id);
    }
    
    List parts = new ArrayList();
    public List getPARTSID(){
        return parts;
    }
    
    public void addPARTSID(String id){
        parts.add(id);
    }
    
    public String currentDate = "0";
    public void currentDATE(String date){
        currentDate = date;
    }
}
