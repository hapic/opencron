<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <style type="text/css">
        .subJobUl li {
            background-color: rgba(0, 0, 0, 0.3);
            border-radius: 1px;
            height: 26px;
            list-style: outside none none;
            margin-top: -27px;
            margin-bottom: 29px;
            margin-left: 100px;
            padding: 4px 15px;
            width: 350px;
        }
    </style>

    <script type="text/javascript" src="${contextPath}/static/js/job.validata.js"></script>
    <script>
        $(document).ready(function () {
            $('#groupId').multiselect();
        });


        function save() {



            if($("#agentId").val()<1){
                opencron.tipError($("#agentIdTips"), "选择执行器!");
                return false;
            }else{
                opencron.tipOk($("#agentIdTips"));
            }


            //判断是否有选择组，如果没有选择，则不运行保存
            if($("#groupId").val()<1){
                opencron.tipError($("#groupTip"), "选择分组!");
                return false;
            }else{
                opencron.tipOk($("#groupTip"));
            }

            if(!$("#commentList").val()){
                opencron.tipError($("#commentListTip"), "作业列表不能为空!");
                return false;
            }else{
                opencron.tipOk($("#commentListTip"));
            }

            var right=true
            ;
            $.ajax({
                headers: {"csrf": '${csrf}'},
                type: "POST",
                url: "${contextPath}/verify/exp.do",
                data: {
                    "cronType": 1,
                    "cronExp": $("#cronExp").val()
                },
                dataType : "json",
                async:false,
                success : function(data) {
                    if (data) {
                        opencron.tipOk($("#expTip"));
                        right=true;
                    } else {
                        opencron.tipError($("#expTip"), "时间规则语法错误!");
                        right=false;
                    }
                }
            });



            return right;
        }
    </script>



</head>

<body>
<!-- Content -->
<section id="content" class="container">

    <!-- Messages Drawer -->
    <jsp:include page="/WEB-INF/layouts/message.jsp"/>

    <!-- Breadcrumb -->
    <ol class="breadcrumb hidden-xs">
        <li class="icon">&#61753;</li>
        当前位置：
        <li><a href="">opencron</a></li>
        <li><a href="">作业管理</a></li>
        <li><a href="">批量添加作业</a></li>
    </ol>
    <h4 class="page-title"><i class="fa fa-plus" aria-hidden="true"></i>&nbsp;批量添加作业</h4>

    <div style="float: right;margin-top: 5px">
        <a onclick="goback();" class="btn btn-sm m-t-10" style="margin-right: 16px;margin-bottom: -4px"><i class="fa fa-mail-reply" aria-hidden="true"></i>&nbsp;返回</a>
    </div>

    <div class="block-area" id="basic">
        <div class="tile p-15 textured">
            <form class="form-horizontal" role="form" id="jobform" action="${contextPath}/job/initJobList.do" enctype="multipart/form-data" method="post" onsubmit="return save()"></br>
                <input type="hidden" name="csrf" value="${csrf}">
                <input type="hidden" name="command" id="command">

                <div class="form-group">
                    <label for="agentId" class="col-lab control-label wid150"><i class="glyphicon glyphicon-leaf"></i>&nbsp;&nbsp;执&nbsp;&nbsp;行&nbsp;&nbsp;器&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <c:if test="${empty agent}">
                            <select id="agentId" name="agentId" class="form-control m-b-10 input-sm">
                                <c:forEach var="d" items="${agents}">
                                    <option value="${d.agentId}">${d.ip}&nbsp;(${d.name})</option>
                                </c:forEach>
                            </select>
                        </c:if>
                        <c:if test="${!empty agent}">
                            <input type="hidden" id="agentId" name="agentId" value="${agent.agentId}">
                            <input type="text" class="form-control input-sm" value="${agent.name}&nbsp;&nbsp;&nbsp;${agent.ip}" readonly>
                            <font color="red">&nbsp;*只读</font>
                        </c:if>
                        <span class="tips" id="agentIdTips">&nbsp;&nbsp;要执行此作业的机器名称和IP地址</span>
                    </div>
                </div>
                <br>
                <div class="form-group">
                    <label for="cronExp" class="col-lab control-label wid150"><i class="glyphicon glyphicon-filter"></i>&nbsp;&nbsp;时间规则&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="cronExp" name="cronExp">
                        <span class="tips" id="expTip" tip="请采用unix/linux的时间格式表达式,如 00 01 * * *">请采用unix/linux的时间格式表达式,如 00 01 * * *</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="groupId" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;分组名称：</label>
                    <div class="col-md-10">
                        <select class="multiselect" title="jobGroupList" id="groupId" name="groupId">
                            <c:if test="${empty param.groupId}">
                                <option value="0" >--先选中分组--</option>
                            </c:if>
                            <c:forEach items="${jobGroupList}" var="group">
                                <option  value="${group.id}" >${group.name}</option>
                            </c:forEach></select>
                        <br/>
                        <span id="groupTip" tip="必填项,作业所在组"></span>
                        <span class="tips" tip="必填项,作业所在组">必填项,作业所在组</span>
                    </div>
                </div><br>


                <div class="form-group">
                    <label for="commentList" class="col-lab control-label wid150"><i class="glyphicon glyphicon-magnet"></i>作业列表</label>
                    <div class="col-md-10">
                        <textarea class="form-control" id="commentList" name="commentList" style="height: 90px;"></textarea>
                        <span id="commentListTip" tip="必填项,作业列表"></span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <div class="col-md-offset-1 col-md-10">
                        <button type="submit" class="btn btn-sm m-t-10"><i class="icon">&#61717;</i>&nbsp;保存</button>&nbsp;&nbsp;
                        <button type="button" onclick="history.back()" class="btn btn-sm m-t-10"><i class="icon">&#61740;</i>&nbsp;取消</button>
                    </div>
                </div>
            </form>
        </div>
    </div>

</section>

</body>

</html>
