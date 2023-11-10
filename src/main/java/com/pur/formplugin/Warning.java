package com.pur.formplugin;


import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.AbstractFormDataModel;
import kd.bos.entity.datamodel.TableValueSetter;
import kd.bos.form.control.Control;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.tmc.cdm.common.enums.DraftBillStatusEnum;
import kd.tmc.cdm.formplugin.index.ExpiredWarnPlugin;
import kd.tmc.fbp.common.enums.BillStatusEnum;
import kd.tmc.fbp.common.helper.TmcDataServiceHelper;
import kd.tmc.fbp.common.helper.TmcOrgDataHelper;
import kd.tmc.fbp.common.util.DateUtils;
import kd.tmc.fbp.common.util.EmptyUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author 84514
 */
public class Warning extends ExpiredWarnPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 监听按钮
        this.addClickListeners("tpv_freshbutton");//查询
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        long curOrgId = RequestContext.get().getOrgId();
        QFilter filter = new QFilter("tpv_org", "=", curOrgId);//组织
        DynamicObject initOrg = QueryServiceHelper.queryOne("tpv_my_purchasereq", "id,tpv_exratetable,tpv_currency", filter.toArray());
        if (initOrg != null) {
            this.getModel().setValue("tpv_ratetable", initOrg.get("tpv_exratetable"));
            this.getModel().setValue("tpv_reportcurrency", initOrg.get("tpv_currency"));
        }

        this.showData();
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        // 获取被点击的控件对象
        Control source = (Control) evt.getSource();
        String key = source.getKey().toLowerCase();
        if ("tpv_freshbutton".equals(key)) {
            this.showData();
        }
    }

    private void showData() {
        DynamicObject[] payableBills = this.getDate();
        AbstractFormDataModel model = (AbstractFormDataModel)this.getModel();
        model.deleteEntryData("tpv_entryentity");//卡片单据体
        DynamicObject rateTable = (DynamicObject)this.getModel().getValue("tpv_ratetable");//卡片汇率表
        DynamicObject reportCurrency = (DynamicObject)this.getModel().getValue("tpv_reportcurrency");//卡片报告币别
        if (rateTable == null) {
            this.getView().showTipNotification(ResManager.loadKDString("汇率表不能为空。", "ExpiredWarnPlugin_1", "tmc-cdm-formplugin", new Object[0]));//插件基类
        } else if (reportCurrency == null) {
            this.getView().showTipNotification(ResManager.loadKDString("报告币别不能为空。", "ExpiredWarnPlugin_2", "tmc-cdm-formplugin", new Object[0]));
        } else if (!EmptyUtil.isEmpty(payableBills)) {
            model.beginInit();
            TableValueSetter vs = new TableValueSetter(new String[0]);
            List<Long> sourceCurrencys = (List) Arrays.asList(payableBills).stream().filter((v) -> {
                return v.getDynamicObject("currency") != null;//币别（采购）
            }).map((v) -> {
                return v.getDynamicObject("currency").getLong("id");
            }).distinct().collect(Collectors.toList());
            Map<Long, BigDecimal> sourceCurrencyMap = new HashMap(10);

            Long sourceCurrency;
            BigDecimal value;
            for(Iterator var8 = sourceCurrencys.iterator(); var8.hasNext(); sourceCurrencyMap.put(sourceCurrency, value)) {
                sourceCurrency = (Long)var8.next();
                value = BaseDataServiceHelper.getExchangeRate(rateTable.getLong("id"), sourceCurrency, reportCurrency.getLong("id"), new Date());
                if (value == null) {
                    value = BigDecimal.ZERO;
                }
            }

            vs.addField("tpv_org", new Object[0]);//采购组织
            vs.addField("tpv_type", new Object[0]);//采购类型
            vs.addField("tpv_use", new Object[0]);//用途
            vs.addField("tpv_billstatus", new Object[0]);//单据状态
            vs.addField("tpv_contract", new Object[0]);//合同号
            vs.addField("tpv_appdate", new Object[0]);//申请日期
            vs.addField("tpv_stacurrency", new Object[0]);//本位币
            vs.addField("tpv_amount", new Object[0]);//金额
            vs.addField("tpv_setcurrency", new Object[0]);//结算金
            vs.addField("tpv_maturity", new Object[0]);//单据到期
            vs.addField("tpv_user", new Object[0]);//申请人
            vs.addField("tpv_full", new Object[0]);//供应商
            vs.addField("tpv_auditor", new Object[0]);//审核人
            DynamicObject[] var12 = payableBills;
            int var13 = payableBills.length;

            for(int var14 = 0; var14 < var13; ++var14) {
                DynamicObject bill = var12[var14];
                vs.addRow(bill.get("tpv_org.id"), bill.get("tpv_type.id"), bill.get("tpv_usage"), bill.get("billstatus"),
                        bill.get("tpv_contract"), bill.get("tpv_applydate"), bill.get("tpv_currency.id"), bill.get("tpv_amoun"), bill.get("tpv_tocurr"),
                        bill.get("tpv_duedate"), bill.get("tpv_applier"), bill.get("tpv_supplier"), bill.get("auditor"),
                        bill.getBigDecimal("tpv_amoun").multiply(bill.getDynamicObject("tpv_currency") != null ? (BigDecimal)sourceCurrencyMap.get(bill.getDynamicObject("tpv_currency").getLong("id")) : BigDecimal.ZERO));
            }

            model.batchCreateNewEntryRow("tpv_entryentity", vs);
            model.endInit();
            this.getView().updateView("tpv_entryentity");
        }
    }

    private DynamicObject[] getDate() {
        int day = (Integer)this.getModel().getValue("day");
        if (day == 0) {
            this.getView().showTipNotification(ResManager.loadKDString("请输入到期天数。", "ExpiredWarnPlugin_0", "tmc-cdm-formplugin", new Object[0]));
            return null;
        } else {
            String appId = this.getModel().getDataEntityType().getAppId();
            Long userPK = Long.valueOf(RequestContext.get().getUserId());
            Set<Long> orgs = new HashSet();
            orgs.addAll(TmcOrgDataHelper.getAuthorizedBankOrgId(userPK, appId, "tpv_my_purchasereq", "47150e89000000ac"));//采购申请
            orgs.addAll(TmcOrgDataHelper.getAuthorizedBankOrgId(userPK, appId, "tpv_my_purchord", "47150e89000000ac"));//采购订单
            QFilter orgfilter = new QFilter("tpv_org", "in", orgs);//组织
            QFilter filters = new QFilter("tpv_duedate", "<=", DateUtils.getNextDay(new Date(), day));//需求日期
//            QFilter filters_pledge = new QFilter("pledgeenddate", "<=", DateUtils.getNextDay(new Date(), day));
            QFilter statusFilters = new QFilter("billstatus", "=", BillStatusEnum.AUDIT.getValue());
            QFilter billFilters = new QFilter("draftbillstatus", "in", new String[]{DraftBillStatusEnum.REGISTERED.getValue(), DraftBillStatusEnum.COLLOCATED.getValue()});
//            QFilter billFiltersOfPledge = new QFilter("draftbillstatus", "=", DraftBillStatusEnum.PLEDGED.getValue());
            DynamicObject[] bills = TmcDataServiceHelper.load("tpv_my_purchord", this.getFields(), new QFilter[]{filters, statusFilters, orgfilter, billFilters}, "tpv_duedate");
//            DynamicObject[] billsOfPledge = TmcDataServiceHelper.load("tpv_my_purchord", this.getFields(), new QFilter[]{filters_pledge, statusFilters, orgfilter, billFiltersOfPledge}, "pledgeenddate");
            List<DynamicObject> allBills = new ArrayList();
//            allBills.addAll(Arrays.asList(bills));
//            allBills.addAll(Arrays.asList(billsOfPledge));
            return allBills.toArray(new DynamicObject[0]);
        }
    }

    private String getFields() {
        return "tpv_org,tpv_billtypefield,tpv_usage,billstatus,tpv_contract,tpv_applydate,tpv_currency,tpv_amount,tpv_tocurr,tpv_duedate,tpv_applier,tpv_supplier,auditor,tpv_duedate";
    }

}
