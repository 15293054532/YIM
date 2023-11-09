package com.pur.test;

import com.pur.formplugin.RelationTool;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.form.FormShowParameter;
import kd.bos.workflow.design.plugin.IWorkflowDesigner;

import java.util.*;

public class ShowWork extends AbstractBillPlugIn implements IWorkflowDesigner {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 监听确认和取消按钮
        this.addClickListeners("tpv_refresh", "tpv_goback");
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        FormShowParameter showParameter = this.getView().getFormShowParameter();
        Map<String, Object> maps = new HashMap<>();
        maps = showParameter.getCustomParams();

        String id = (String) maps.get("id");

    }

    @Override
    public Map<String, Object> getDesignerInitData(Map<String, Object> map) {
        List<RelationTool> nodelList = new ArrayList<>();
        List<String> targetList = new ArrayList<>();
        targetList.add("nodeId-2");
        targetList.add("nodeId-3");
        targetList.add("nodeId-4");
        List<String> targetList2 = new ArrayList<>();
        targetList2.add("nodeId-5");
        nodelList.add(new RelationTool("nodeId-1", "采购申请", "node1SubTitle", "null", "null", "null", null, targetList, 1));
        nodelList.add(new RelationTool("nodeId-2", "采购订单", "node2SubTitle", "null", "null", "null", "nodeId-1", targetList2, 2));
        nodelList.add(new RelationTool("nodeId-3", "采购收货单", "node3SubTitle", "null", "null", "null", "nodeId-1", null, 2));
        nodelList.add(new RelationTool("nodeId-4", "采购入库单", "node4SubTitle", "null", "null", "null", "nodeId-1", null, 2));
        nodelList.add(new RelationTool("nodeId-5", "付款单", "node5SubTitle", "null", "null", "null", "nodeId-2", null, 3));
        List<RelationTool> relation = calcPosition(nodelList);
        String getXml = convertNodeToXml(relation);
        map.put("graph_xml", getXml);
        return map;
    }

    private String convertNodeToXml(List<RelationTool> nodeList) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<mxGraphModel grid=\"0\">\n" +
                "    <root>\n" +
                "        <mxCell id=\"node_0\"/>\n" +
                "        <mxCell id=\"node_1\" type=\"Diagram\" group=\"ProcessControl\" parent=\"node_0\">\n" +
                "            <Object process_id=\"bill_circulaterelation\" as=\"properties\"/>\n" +
                "        </mxCell>");

        nodeList.forEach(node ->
        {
            xml.append("<mxCell id=\"" + node.getNodeId() + "\" value=\"\"\n" +
                    "                style=\"shape=billCard;whiteSpace=wrap;spacingLeft=50;spacingRight=10;overflow=hidden;resizable=0;\"\n" +
                    "                type=\"billCard\" parent=\"node_1\" vertex=\"1\" showRecords=\"false\">\n" +
                    "            <mxGeometry width=\"216.0\" height=\"132.0\" " + "x=\"" + node.getX() + "\" " + "y=\"" + node.getY() + "\" as=\"geometry\"/>\n" +
                    "            <Object as=\"properties\"\n" +
                    "                    title=\"" + node.getTitle() + "\"\n" +
                    "                    subtitle=\"" + node.getSubTitle() + "\"\n" +
                    "                    name=\"" + node.getInfo1() + "\"\n" +
                    "                    department=\"" + node.getInfo2() + "\"\n" +
                    "                    status=\"" + node.getInfo3() + "\"\n" +
                    "                    />\n" +
                    "        </mxCell>");
            if (node.getTargetNodeId() != null && !node.getTargetNodeId().isEmpty()) {
                for (int i = 0; i < node.getTargetNodeId().size(); i++) {
                    xml.append(
                            "<mxCell id=\"node_line_" + node.getNodeId() + i + "\"\n" +
                                    "                style=\"edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;jettySize=auto;orthogonalLoop=1;entryX=0;entryY=0.5;strokeColor=#A1CFFF!important;;\"\n" +
                                    "                type=\"SequenceFlow\" \n" +
                                    "                parent=\"node_1\" \n" +
                                    "                edge=\"1\" \n" +
                                    "                source=\"" + node.getNodeId() + "\"\n" +
                                    "                target=\"" + node.getTargetNodeId().get(i) + "\">\n" +
                                    "            <mxGeometry relative=\"1\" as=\"geometry\"/>\n" +
                                    "        </mxCell>");
                }
            }
        });

        xml.append("</root>\n" +
                "</mxGraphModel>");
        return xml.toString();
    }

    private List<RelationTool> calcPosition(List<RelationTool> nodelList) {
        int currentX = 0;
        int currentY = 0;
        int currentLevel = 1;

        for (RelationTool node : nodelList) {
            int level = node.getLevel();

            if (level > currentLevel) {
                // 进入下一层，重置 x 和 y
                currentLevel = level;
                currentX += 300;
                currentY = 0;
            }

            node.setX(currentX);
            node.setY(currentY);

            currentY += 200;
        }
        return nodelList;
    }
}
