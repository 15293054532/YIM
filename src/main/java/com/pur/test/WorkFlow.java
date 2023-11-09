package com.pur.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pur.entity.Relation;
import com.pur.tool.ConnectGraphUtil;
import com.pur.formplugin.RelationTool;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.workflow.design.plugin.IWorkflowDesigner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkFlow extends AbstractBillPlugIn implements IWorkflowDesigner {

    @Override
    public Map<String, Object> getDesignerInitData(Map<String, Object> map) {
        // 获取树形单据体数据
        JSONArray treeEntity = this.getView().getFormShowParameter().getCustomParam("entity");
        List<Relation> relations = convert(treeEntity);
        ConnectGraphUtil.createRelation(relations);
        StringBuilder relationXml = new StringBuilder();
        String xml = spliceXml(relations, relationXml);
        map.put("graph_xml", xml);
        return map;
    }

    private String spliceXml(List<Relation> relations, StringBuilder xml) {
        for (Relation relation : relations) {
            xml.append(spliceModel(relation));
            if (relation.getParentId() != null && relation.getParentId() != 0) {
                xml.append(spliceLine(relation));
            }
            List<Relation> targets = relation.getTargets();
            if (!targets.isEmpty()) {
                spliceXml(targets, xml);
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<mxGraphModel grid=\"0\">" +
                "<root>" +
                "    <mxCell id=\"relation_0\"/>" +
                "    <mxCell id=\"relation_1\" type=\"Diagram\" group=\"ProcessControl\" parent=\"relation_0\">" +
                "        <Object process_id=\"bill_circulaterelation\" as=\"properties\"/>" +
                "    </mxCell>" +
                xml.toString() +
                "</root>" +
                "</mxGraphModel>";
    }

    private String spliceLine(Relation relation) {
        return "<mxCell id=\"relation_line_" + relation.getId() + "\"" +
                "        style=\"edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;jettySize=auto;orthogonalLoop=1;strokeColor=#A1CFFF!important;;\"" +
                "        type=\"SequenceFlow\" " +
                "        parent=\"relation_1\" " +
                "        edge=\"1\" " +
                "        source=\"" + relation.getParentId() + "\"" +
                "        target=\"" + relation.getId() + "\">" +
                "    <mxGeometry relative=\"1\" as=\"geometry\"/>" +
                "</mxCell>";
//                "<mxCell id=\"relation_line_" + relation.getId() + "_child" +"\"" +
//                "       parent=\"relation_line_" + relation.getId() + "\"" +
//                "       vertex=\"1\" " +
//                "       style=\"shape=ierp.billrelation.IconComplete;\"/>" +
//                "       <Object entityNumber=\"tpv_my_purchasereq\" businessKey =\"1810250716527200256\" type =\"complete\" " +
//                "       title=\"下推已完成\" as =\"properties\"/>" +
//                "       <mxGeometry width=\"24.0\" height=\"24.0\" x = \"0.0\" y = \"0.0\" relative = \"true\" as = \"geometry\">" +
//                "           <mxPoint x =\"-32.0\" y = \"-12.0\" as = \"offset\" / >" +
//                "       </mxGeometry>" +
//                "</mxCell>";
    }

    private String spliceModel(Relation relation) {
        String style = "shape=billCard";
        return "<mxCell id=\"" + relation.getId() + "\"" +
                " value=\"" + "\"" +
                " style=\"" + style + ";whiteSpace=wrap;spacingLeft=50;spacingRight=10;overflow=hidden;resizable=0\"" +
                " type=\"billCard\" parent=\"relation_1\" vertex=\"1\" showRecords=\"false\" clickable=\"false\">" +
                "<mxGeometry width=\"" + relation.getWidth() + "\"" +
                " height=\"" + relation.getHeight() + "\"" +
                " x=\"" + relation.getX() + "\"" +
                " y=\"" + relation.getY() + "\" as=\"geometry\"/>" +
                "<Object as=\"properties\"" +
                "        title=\"" + relation.getTitle() + "\"" +
                "        subtitle=\"" + relation.getSubtitle() + "\"" +
                "        name=\"" + "单价：" + relation.getName() + "\"" +
                "        department=\"" + relation.getDepartment() + "\"" +
                "        status=\"" + relation.getStatus() + "\"" +
                "        />" +
                "</mxCell>";
    }

    public static List<Relation> convert(JSONArray array) {
        List<Relation> relations = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {

            JSONObject obj = array.getJSONObject(i);

            Relation relation = new Relation();
            if (obj.getLong("parentId") == null || obj.getLong("parentId") == 0) {
                relation.setParentId(11111L);
            }
            relation.setId(obj.getLong("id"));
            // 显示数据
            if (obj.containsKey("tpv_product")) {
                relation.setTitle(obj.get("tpv_product").toString());
            } else {
                relation.setTitle("高新刚");
            }
            if (obj.containsKey("tpv_integer")) {
                relation.setSubtitle(obj.get("tpv_integer").toString());
            } else {
                relation.setSubtitle("null");
            }
            if (obj.containsKey("tpv_unitprice")) {
                relation.setName(obj.get("tpv_unitprice").toString());
            } else {
                relation.setName("null");
            }
            if (obj.containsKey("null")) {
                relation.setDepartment(obj.get("null").toString());
            } else {
                relation.setDepartment("null");
            }
            if (obj.containsKey("seq")) {
                relation.setStatus(obj.get("seq").toString());
            } else {
                relation.setStatus("null");
            }

            relation.setParentId(obj.getLong("pid"));
            relation.setTargets(new ArrayList<>());
            relation.setVirtual(false);
            relation.setHeight(132);
            relation.setWidth(216);

            relations.add(relation);
        }

        // 将子节点添加到父节点中
        for (Relation relation : relations) {
            long parentId = relation.getParentId();
            if (parentId != 0) {
                for (Relation parent : relations) {
                    if (parent.getId() == parentId) {
                        parent.getTargets().add(relation);
                        break;
                    }
                }
            }
        }

        Relation virtualRoot = new Relation();
        virtualRoot.setId(11111L);
        virtualRoot.setVirtual(true);
        virtualRoot.setWidth(0);
        virtualRoot.setHeight(0);
        ArrayList<Relation> rootNodeTarget = new ArrayList<>();
        for (Relation relation : relations) {
            if (relation.getParentId() == 0) {
                rootNodeTarget.add(relation);
            }
        }
        virtualRoot.setTargets(rootNodeTarget);
        relations.add(0, virtualRoot);
        return relations;
    }
}