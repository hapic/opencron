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

    <script type="text/javascript">
        function save() {

            //判断是否有选择组，如果没有选择，则不运行保存
            if($("#groupId").val()<1){
                return false;
            }
        }

        $(document).ready(function () {
            opencronValidata = new Validata('${contextPath}','${csrf}');

            $('#groupId').multiselect();

            $('#dependenceid').multiselect();
            $('#weight').multiselect();



            $("#groupId").change(function () {//如果选择发生了变化
                refreshMultiSelect($(this).val());
            });



            <c:if test="${!empty param.groupId}">
                refreshMultiSelect(${param.groupId})
            </c:if>
        });


        //刷新当前组下的job
        function refreshMultiSelect(groupId) {
            $.ajax({
                type : "POST",
                url : "${contextPath}/job/loadJobByGroupId.do",
                data : {groupId:groupId,csrf:'${csrf}'},
                dataType : "json",
                success : function(json) {
                    $("#dependenceid").html("");
                    for (var i = 0; i < json.length; i++) {
                        $("#dependenceid").append("<option value='" + json[i].jobId + "'>" + json[i].jobName + "</option>");
                    }
                    $("#dependenceid").multiselect("destroy").multiselect({
                        // 自定义参数，按自己需求定义
                        nonSelectedText : '----请选择----',
                        maxHeight : 350,
//                        includeSelectAllOption : true,
                        numberDisplayed : 5,
                        onChange: function(option, checked, select) {
                            //判断选择的是什么运行模式
                            var selectedOptions = $('#dependenceid option:selected');
                            if(selectedOptions.length>0){//如果有选择依赖任务
                                opencron.tipDefault("#cronExp");
                                $(".execTypeDiv").hide();
                                $(".timeExpDiv").hide();
                            }else{
                                $(".execTypeDiv").show();

                                var execTypeVal=$("input[name='execType']:checked").val();
                                if(execTypeVal==0){//如果选择的是自动
                                    $(".cronExpDiv").show();
                                }else{
                                    $(".cronExpDiv").hide();
                                }

                            }


                        }
                    });
                }
            });
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
        <li><a href="">添加作业</a></li>
    </ol>
    <h4 class="page-title"><i class="fa fa-plus" aria-hidden="true"></i>&nbsp;添加作业</h4>

    <div style="float: right;margin-top: 5px">
        <a onclick="goback();" class="btn btn-sm m-t-10" style="margin-right: 16px;margin-bottom: -4px"><i class="fa fa-mail-reply" aria-hidden="true"></i>&nbsp;返回</a>
    </div>

    <div class="block-area" id="basic">
        <div class="tile p-15 textured">
            <form class="form-horizontal" role="form" id="jobform" action="${contextPath}/job/save.do" method="post" onsubmit="return save()"></br>
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
                        <span class="tips">&nbsp;&nbsp;要执行此作业的机器名称和IP地址</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="jobName" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;作业名称&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="jobName" name="jobName">
                        <span class="tips" tip="必填项,该作业的名称">必填项,该作业的名称</span>
                    </div>
                </div>
                <br>
                <div class="form-group">
                    <label for="jobName" class="col-lab control-label wid150">
                        <i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;权重&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <select class="multiselect" title="jobGroupList" id="weight" name="weight">
                            <c:forEach var="i" begin="0" end="5" step="1">
                                <option value="${i}">${i}</option>
                            </c:forEach>
                        </select>
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
                    <label for="dependenceid" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;前置依赖：</label>
                    <select class="multiselect" title="test" multiple="multiple" id="dependenceid" name="dependenceid">
                    </select>
                </div><br>

                <div class="form-group execTypeDiv">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-info-sign"></i>&nbsp;&nbsp;运行模式&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="execType0" class="radio-label"><input type="radio" name="execType" id="execType0" value="0" checked>自动&nbsp;&nbsp;&nbsp;</label>
                        <label for="execType1" class="radio-label"><input type="radio" name="execType" id="execType1"  value="1">手动</label>&nbsp;&nbsp;&nbsp;
                        <br><span class="tips" id="execTypeTip" tip="">自动模式:执行器自动执行</span>
                    </div>
                </div>
                <br>

                <div class="form-group cronExpDiv">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-bookmark"></i>&nbsp;&nbsp;规则类型&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="cronType0" class="radio-label"><input type="radio" name="cronType" value="0" id="cronType0" checked>crontab&nbsp;&nbsp;&nbsp;</label>
                        <label for="cronType1" class="radio-label"><input type="radio" name="cronType" value="1" id="cronType1">quartz</label>&nbsp;&nbsp;&nbsp;
                        </br><span class="tips" id="cronTip" tip="crontab: unix/linux的时间格式表达式">crontab: unix/linux的时间格式表达式</span>
                    </div>
                </div>
                <br>

                <div class="form-group cronExpDiv timeExpDiv">
                    <label for="cronExp" class="col-lab control-label wid150"><i class="glyphicon glyphicon-filter"></i>&nbsp;&nbsp;时间规则&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="cronExp" name="cronExp">
                        <span class="tips" id="expTip" tip="请采用unix/linux的时间格式表达式,如 00 01 * * *">请采用unix/linux的时间格式表达式,如 00 01 * * *</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="cmd" class="col-lab control-label wid150"><i class="glyphicon glyphicon-th-large"></i>&nbsp;&nbsp;执行命令&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <textarea class="form-control input-sm" id="cmd" style="height:200px;resize:vertical"></textarea>
                        <span class="tips" tip="请采用unix/linux的shell支持的命令">请采用unix/linux的shell支持的命令</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="runAs" class="col-lab control-label wid150"><i class="glyphicons glyphicons-user"></i>&nbsp;&nbsp;运行身份&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="runAs" name="runAs" value="root">
                        <span class="tips" tip="该任务以哪个身份执行(默认是root)">该任务以哪个身份执行(默认是root)</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="successExit" class="col-lab control-label wid150"><i class="glyphicons glyphicons-tags"></i>&nbsp;&nbsp;成功标识&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="successExit" name="successExit" value="0">
                        <span class="tips" tip="自定义作业执行成功的返回标识(默认执行成功是0)">自定义作业执行成功的返回标识(默认执行成功是0)</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label class="col-lab control-label wid150"><i class="glyphicon  glyphicon glyphicon-forward"></i>&nbsp;&nbsp;失败重跑&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="redo01" class="radio-label"><input type="radio" name="redo" value="1" id="redo01">是&nbsp;&nbsp;&nbsp;</label>
                        <label for="redo00" class="radio-label"><input type="radio" name="redo" value="0" id="redo00" checked>否</label>&nbsp;&nbsp;&nbsp;
                        <br><span class="tips">执行失败时是否自动重新执行</span>
                    </div>
                </div>
                <br>

                <div class="form-group countDiv">
                    <label for="runCount" class="col-lab control-label wid150"><i class="glyphicon glyphicon-repeat"></i>&nbsp;&nbsp;重跑次数&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="runCount" name="runCount">
                        <span class="tips" tip="执行失败时自动重新执行的截止次数">执行失败时自动重新执行的截止次数</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="timeout" class="col-lab control-label wid150"><i class="glyphicon glyphicon-ban-circle"></i>&nbsp;&nbsp;超时时间&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="timeout" name="timeout" value="0">
                        <span class="tips" tip="执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)">执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)</span>
                    </div>
                </div>
                <br>
                <div class="form-group">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-warning-sign"></i>&nbsp;&nbsp;失败报警&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="warning1" class="radio-label"><input type="radio" name="warning" value="1" id="warning1" checked>是&nbsp;&nbsp;&nbsp;</label>
                        <label for="warning0" class="radio-label"><input type="radio" name="warning" value="0" id="warning0">否</label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                        </br><span class="tips" tip="任务执行失败时是否发信息报警">任务执行失败时是否发信息报警</span>
                    </div>
                </div>
                <br>

                <div class="form-group contact">
                    <label for="mobiles" class="col-lab control-label wid150"><i class="glyphicon glyphicon-comment"></i>&nbsp;&nbsp;报警手机&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="mobiles" name="mobiles">
                        <span class="tips" tip="任务执行失败时将发送短信给此手机,多个请以逗号(英文)隔开">任务执行失败时将发送短信给此手机,多个请以逗号(英文)隔开</span>
                    </div>
                </div>
                <br>

                <div class="form-group contact">
                    <label for="email" class="col-lab control-label wid150"><i class="glyphicon glyphicon-envelope"></i>&nbsp;&nbsp;报警邮箱&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="email" name="emailAddress">
                        <span class="tips" tip="任务执行失败时将发送报告给此邮箱,多个请以逗号(英文)隔开">任务执行失败时将发送报告给此邮箱,多个请以逗号(英文)隔开</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="comment" class="col-lab control-label wid150"><i class="glyphicon glyphicon-magnet"></i>&nbsp;&nbsp;描&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;述&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <textarea class="form-control input-sm" id="comment" name="comment" style="height: 50px;"></textarea>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <div class="col-md-offset-1 col-md-10">
                        <button type="button" id="save-btn" class="btn btn-sm m-t-10"><i class="icon">&#61717;</i>&nbsp;保存</button>&nbsp;&nbsp;
                        <button type="button" onclick="history.back()" class="btn btn-sm m-t-10"><i class="icon">&#61740;</i>&nbsp;取消</button>
                    </div>
                </div>
            </form>
        </div>
    </div>

</section>

</body>

</html>
