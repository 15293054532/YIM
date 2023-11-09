package com.pur.formplugin;

import com.alibaba.druid.util.StringUtils;
import kd.bos.base.AbstractBasePlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.OrmLocaleValue;
import kd.bos.entity.datamodel.events.AfterDeleteRowEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * @author 84514
 */
public class CaiGousq extends AbstractBasePlugIn {

    @Override
    public void propertyChanged(PropertyChangedArgs changedArgs) {//开启及时触发值，字段值改变时触发事件
        String propertyName = changedArgs.getProperty().getName();
        ChangeData cd = changedArgs.getChangeSet()[0];
        //获取分录行号
        int rowIndex = cd.getRowIndex();
        //锁定
        this.getView().setEnable(false, "billno");
        switch (propertyName) {
            //监听申请数量
            case "tpv_applyqty":
                //申请数量变化，金额改变
                qtyAlter(cd);
                break;
            //建议采购单价
            case "tpv_price":
                priceChange(cd);
                break;
            //计量单位
            case "tpv_unit":
            case "tpv_unitmea":
                getBaseunitqty(rowIndex);
                break;
            //供应商
            case "tpv_supplier":
                //清空单据体
                this.getModel().deleteEntryData("tpv_my_purchasereq_sp");
                break;
            //物料
            case "tpv_materia":
                materiaChange(cd);
                break;
            //金额
            case "tpv_amount":
                amountcollect();//金额汇总
                break;
            case "tpv_applier":
                carryDepartment();
                break;
            //监听本位币，汇率表，结算币，汇率日期
            case "tpv_currency":
            case "tpv_tocurr":
            case "tpv_exratetable":
            case "tpv_exratedate":
                showExchangerate();
                break;
            default:
                break;
        }

    }

    /**
     * 判断申请人是否改变
     */
    private void carryDepartment() {
        //获取申请人的值
        DynamicObject applier =  (DynamicObject)this.getModel().getValue("tpv_applier");
        //判断申请人是否为空
        if (applier != null){
            //获取申请人对象
            DynamicObjectCollection entryentity = applier.getDynamicObjectCollection("entryentity");
            if (!entryentity.isEmpty()){
                for (DynamicObject dynamicObject : entryentity) {
                    DynamicObject dpt = dynamicObject.getDynamicObject("dpt");
                    long id = dpt.getLong("id");
                    //判断部门是否为空
                    this.getModel().setValue("tpv_applyorg",id);
                }
            }else {
                this.getModel().setValue("tpv_applyorg",null);
            }
        }else {
            this.getModel().setValue("tpv_applyorg",null);
        }
    }

    /**
     * 获取汇率
     */
    private void showExchangerate() {
        //获取汇率表
        DynamicObject exratetable = (DynamicObject) this.getModel().getValue("tpv_exratetable");
        //获取目标币
        DynamicObject fromcurr = (DynamicObject) this.getModel().getValue("tpv_tocurr");
        //获取本位币
        DynamicObject tocurr = (DynamicObject) this.getModel().getValue("tpv_currency");
        //获取申请日期
        Date applyDate = (Date) this.getModel().getValue("tpv_exratedate");
        //判断目标币和本位币是否相同
        if (exratetable != null && fromcurr != null && tocurr != null & applyDate != null) {
            if (fromcurr.equals(tocurr)) {
                this.getModel().setValue("tpv_exrate", BigDecimal.ONE);
                return;
            }
            //获得汇率表、目标币、本位币的Id
            Long exratetableId = exratetable.getLong("id");
            Long fromcurrId = fromcurr.getLong("id");
            Long tocurrId = tocurr.getLong("id");
            if (fromcurrId == null || tocurrId == null) {
                return;
            }
            //计算汇率
            BigDecimal exchangeRate = BaseDataServiceHelper.getExchangeRate(exratetableId, fromcurrId, tocurrId, applyDate);
            this.getModel().setValue("tpv_exrate", exchangeRate);
        } else {
            this.getModel().setValue("tpv_exrate", BigDecimal.ZERO);
        }
    }

    private void amountcollect() {
        //获得分录
        DynamicObjectCollection entrys = this.getModel().getEntryEntity("tpv_my_purchasereq_sp");
        BigDecimal amount = BigDecimal.ZERO;
        //循环分录 计算每行金额的汇总
        for (DynamicObject entry : entrys) {
            amount = amount.add(entry.getBigDecimal("tpv_amount"));
        }
        //将汇总金额赋值表头总金额字段
        this.getModel().setValue("tpv_amoun", amount);
    }

    /**
     * 物料携带规格型号和计量单位
     */
    private void materiaChange(ChangeData cd) {
        //行号
        int index = cd.getRowIndex();
        if (cd.getNewValue() != null) {
            //获取多选基础资料数据id（物料）
            long materiaId = (long) ((DynamicObject) cd.getNewValue()).getPkValue();
            //查询物料信息
            DynamicObject material = BusinessDataServiceHelper.loadSingle(materiaId, "bd_material");
            //获取规格型号
            OrmLocaleValue modelnum = (OrmLocaleValue) material.get("modelnum");
            //判断规格型号是否为空，为空以长宽高填写
            String str = modelnum.getLocaleValue();
            if (StringUtils.isEmpty(str) || str.isEmpty()) {
                BigDecimal length = (BigDecimal) material.get("length");
                BigDecimal width = (BigDecimal) material.get("width");
                BigDecimal height = (BigDecimal) material.get("height");
                if (length != null && width != null && height != null && length.compareTo(BigDecimal.ZERO) > 0
                        && width.compareTo(BigDecimal.ZERO) > 0 && height.compareTo(BigDecimal.ZERO) > 0) {
                    String model = length.longValue() + "*" + width.longValue() + "*" + height.longValue();
                    this.getModel().setValue("tpv_model", model, index);
                }
            } else {
                this.getModel().setValue("tpv_model", modelnum, index);
            }
            //获取计量单位
            DynamicObject unit = (DynamicObject) material.get("baseunit");
            //获取计量单位id
            Object unitId = unit.getPkValue();
            this.getModel().setValue("tpv_unit", unitId, index);
        } else {
            //值置空，可以触发前端非空校验
            this.getModel().setValue("tpv_model", null, index);
            this.getModel().setValue("tpv_unit", null, index);
        }

    }

    /**
     * 申请数量改变,计算基本单位数量
     */
    private void getBaseunitqty(Integer row) {
        //申请数量
        BigDecimal num = (BigDecimal) this.getModel().getValue("tpv_applyqty");

        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            this.getModel().setValue("tpv_basicquan", null, row);
            this.getModel().setValue("tpv_applyqty", null, row);
            return;
        }

        //物料
        DynamicObject materiel = (DynamicObject) this.getModel().getValue("tpv_materia");
        //计量单位
        DynamicObject srcUnit = (DynamicObject) this.getModel().getValue("tpv_unit");
        //基本计量单位
        DynamicObject desUnit = (DynamicObject) this.getModel().getValue("tpv_unitmea");
        if (materiel != null && srcUnit != null && desUnit != null) {
            Long materielId = materiel.getLong("id");
            Long srcUnitId = srcUnit.getLong("id");
            Long desUnitId = desUnit.getLong("id");
            //计算转换率
            BigDecimal rate = getUnitRateConv(materielId, srcUnitId, desUnitId);
            //计算单位数量,乘法
            BigDecimal basenum = num.multiply(rate);
            this.getModel().setValue("tpv_basicquan", basenum, row);
        } else {
            this.getModel().setValue("tpv_basicquan", BigDecimal.ZERO, row);
        }
    }

    /**
     * 计算单位间的转换率
     */
    public static BigDecimal getUnitRateConv(Long materialId, Long srcUnitId, Long desUnitId) {
        BigDecimal unitRate = null;
        if (materialId == null || srcUnitId == null || desUnitId == null) {
            unitRate = BigDecimal.ZERO;
        } else if (srcUnitId == (long) desUnitId) {
            unitRate = BigDecimal.ONE;
        } else {
            final DynamicObject muConv = BaseDataServiceHelper.getMUConv(materialId, srcUnitId, desUnitId);
            if (muConv != null && muConv.getInt("numerator") != 0) {
                unitRate = new BigDecimal(muConv.getInt("numerator")).divide(new BigDecimal(muConv.getInt("denominator")), 10, RoundingMode.HALF_UP);
            }
        }
        if (unitRate == null) {
            unitRate = BigDecimal.ZERO;
        }
        return unitRate;
    }

    /**
     * 建议采购单价发生变化，计算金额
     */
    private void priceChange(ChangeData cd) {
        //判断采购单价改变后是否为空
        if (cd.getNewValue() == null) {
            //金额置空
            this.getModel().setValue("tpv_amount", null);
            //采购单价不可以小于0
        } else if (((BigDecimal) cd.getNewValue()).longValue() <= 0) {
            this.getView().showMessage("注意建议采购单价不能小于等于0");
            //获取单价
            double priceValue = Double.parseDouble(cd.getNewValue().toString());
            //获得申请数量
            double baseunitqty = Double.parseDouble(this.getModel().getValue("tpv_applyqty").toString());
            //计算金额
            double amount = priceValue * baseunitqty;
            BigDecimal bigDecimal = BigDecimal.valueOf(amount) == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount);
            //赋值金额
            this.getModel().setValue("tpv_amount", bigDecimal);
        } else {
            long priceValue = ((BigDecimal) cd.getNewValue()).longValue();
            double baseunitqty = Double.parseDouble(this.getModel().getValue("tpv_applyqty").toString());
            double amount = priceValue * baseunitqty;
            BigDecimal bigDecimal = BigDecimal.valueOf(amount) == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount);
            this.getModel().setValue("tpv_amount", bigDecimal);
        }
    }

    /**
     * 申请数量发生变化，计算金额
     */
    private void qtyAlter(ChangeData cd) {
        //行号
        int index = cd.getRowIndex();
        //判断采购单价是否为空
        if (cd.getNewValue() == null) {
            //金额置空
            this.getModel().setValue("tpv_amount", null);
            //采购单价不可以小于0
        } else {
            long priceLong = ((BigDecimal) cd.getNewValue()).longValue();
            double qty = Double.parseDouble(this.getModel().getValue("tpv_price").toString());
            double amount = priceLong * qty;
            this.getModel().setValue("tpv_amount",amount,index);
        }

    }

    @Override
    public void afterDeleteRow(AfterDeleteRowEventArgs e) {
        //通过单据体行删除事件来触发
        String entryName = e.getEntryProp().getName();
        if ("tpv_my_purchasereq_sp".equals(entryName)) {
            amountcollect();
        }
    }
}
