package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.algo.*;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 84514
 */
public class JointInves extends AbstractReportListDataPlugin {
    @Override
    public DataSet query(ReportQueryParam param, Object o) throws Throwable {
        FilterInfo filterInfo = param.getFilter();
        //申请单号
        String bill = filterInfo.getString("tpv_bill");
        //单据状态
        String billstatus = filterInfo.getString("tpv_billstatus");

        List<QFilter> list = new ArrayList<>();
        if (!StringUtils.isEmpty(bill)) {
            list.add(new QFilter("billno",  QCP.like, "%"+bill+"%"));
        }
        if (!StringUtils.isEmpty(billstatus)) {
            list.add(new QFilter("billstatus", QCP.equals, billstatus));
        }
        /*
         * 采购
         */
        String selelctFiles = "billno tpv_billno,"
                +"'采购' tpv_text,"
                + "tpv_applier tpv_applier,"
                + "tpv_applydate tpv_applydate,"
                + "tpv_my_purchasereq_sp.tpv_materia.name tpv_materialname,"
                + "tpv_my_purchasereq_sp.tpv_materia.modelnum tpv_model,"
                + "tpv_my_purchasereq_sp.tpv_unit tpv_unit,"
                + "tpv_my_purchasereq_sp.tpv_applyqty tpv_applyqty,"
                + "tpv_my_purchasereq_sp.tpv_price tpv_price,"
                + "tpv_my_purchasereq_sp.tpv_amount tpv_amount,"
                + "id tpv_textid"; // 查询字段
        DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), "tpv_my_purchasereq", selelctFiles,
                list.toArray(new QFilter[]{}),"billno desc");
        dataSet.select("tpv_billno","tpv_applier","tpv_applydate","tpv_model","tpv_unit",
                "tpv_applyqty","tpv_price","tpv_amount","tpv_text");

        /*
         * 销售
         */
        String selelctFile = "billno tpv_billno,"
                +"'销售' tpv_text,"
                + "tpv_applier tpv_applier,"
                + "tpv_applydate tpv_applydate,"
                + "tpv_my_salesapp_xs.tpv_materia.name tpv_materialname,"
                + "tpv_my_salesapp_xs.tpv_materia.modelnum tpv_model,"
                + "tpv_my_salesapp_xs.tpv_unit tpv_unit,"
                + "tpv_my_salesapp_xs.tpv_basicquan tpv_applyqty,"
                + "tpv_my_salesapp_xs.tpv_price tpv_price,"
                + "tpv_my_salesapp_xs.tpv_amount tpv_amount,"
                + "id tpv_textid";
        // 查询字段
        DataSet dataSet1 = QueryServiceHelper.queryDataSet(this.getClass().getName(), "tpv_my_salesapp", selelctFile,
                list.toArray(new QFilter[]{}),"billno desc");
        dataSet1.select("tpv_billno","tpv_applier","tpv_applydate",
                "tpv_model","tpv_unit","tpv_applyqty","tpv_price","tpv_amount","tpv_text");

        DataSet officialData = dataSet.union(dataSet,dataSet1);

        /*
         * 订单
         */
        String selelctFile1 = "billno tpv_billno,"
                + "tpv_applier.name tpv_applier1,"
                + "tpv_paytype tpv_paytype,"
                + "tpv_billtypefield tpv_billtypefield,"
                + "tpv_my_purchasereq_sp.tpv_materia.name tpv_materialname,"
                + "tpv_my_purchasereq_sp.tpv_materia.modelnum tpv_model,"
                + "tpv_my_purchasereq_sp.tpv_unit tpv_unit,"
                + "tpv_my_purchasereq_sp.tpv_applyqty tpv_decimal,"
                + "tpv_my_purchasereq_sp.tpv_price tpv_decimal1,"
                + "tpv_my_purchasereq_sp.tpv_amount tpv_decimalfield,"
                + "tpv_my_purchasereq_sp.tpv_text tpv_supplier,"
                + "tpv_my_purchasereq_sp.tpv_addre.name tpv_address,"
                + "tpv_billtypefield.name tpv_source,"
                + "tpv_bigintid tpv_textid";
        DataSet dataSet2 = QueryServiceHelper.queryDataSet(this.getClass().getName(), "tpv_my_purchord", selelctFile1,
                list.toArray(new QFilter[]{}),"billno desc");

        JoinDataSet join = officialData.leftJoin(dataSet2);
        DataSet resultData = join.on("tpv_textid", "tpv_textid")
                .select(new String[] {
                        "tpv_billno","tpv_applier","tpv_applydate",
                        "tpv_model","tpv_unit","tpv_applyqty","tpv_price","tpv_amount","tpv_text",
                        "tpv_materialname"},
                new String[]{"tpv_paytype","tpv_applier1","tpv_billtypefield","tpv_supplier","tpv_address","tpv_source","tpv_decimal",
                        "tpv_decimal1","tpv_decimalfield"
                }).finish();


//        DynamicObjectCollection dynamicObjects = ORM.create().toPlainDynamicObjectCollection(dataSet.copy());
//		for (DynamicObject dynamicObject : dynamicObjects) {
//			String a=dynamicObject.getString("tpv_billno");
//			System.out.println("a");
//		}
        return resultData.distinct();
    }
}

