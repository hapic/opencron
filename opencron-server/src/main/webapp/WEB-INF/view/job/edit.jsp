<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron"  uri="http://www.opencron.org"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <style type="text/css">
        .subJobUl li{
            background-color: rgba(0,0,0,0.3);
            border-radius: 1px;
            height: 26px;
            list-style: outside none none;
            margin-top: -27px;
            margin-bottom: 29px;
            margin-left: 100px;
            padding: 4px 15px;
            width: 350px;
        }

        .delSubJob{
            float:right;margin-right:2px
        }
    </style>

    <script type="text/javascript" src="${contextPath}/static/js/job.validata.js"></script>

    <script type="text/javascript">


        function save() {
           return checkCycle();
        }


        /**
         * 验证当前选择的是否有循环依赖
         */
        function checkCycle() {
            var selected = [];
            $('#dependenceid option:selected').each(function() {
                selected.push($(this).val());
            });
            var data={
                csrf:'${csrf}',
                jobId:${job.jobId},
                dependenceid:selected
            };
            var goon=false;
            $.ajax({
                type : "POST",
                url : "${contextPath}/job/checkCycle.do",
                data : data,
                dataType : "json",
                async : false,
                traditional:true,
                success : function(cycle) {
                    if(cycle){
                        $("#save-btn").attr("disabled", true);
                        swal({
                            title: "出问题了",
                            text: "依赖任务会出现环路",
                            type: "error"
                        });

                    }else{
                        $("#save-btn").attr("disabled", false);
                        swal({
                            title: "",
                            text: "可以保存",
                            type: "success"
                        });
                    }
                    goon= !cycle;
                }
            });
            return goon;
        }

        $(document).ready(function () {
            opencronValidata = new Validata('${contextPath}','${csrf}','${job.jobId}');
            $('#dependenceid').multiselect({
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


            $("#checkCycle").unbind("click");
            $("#checkCycle").bind("click",checkCycle);

            <c:if test="${fn:length(dependenceMap)>0}">
                opencron.tipDefault("#cronExp");
                $(".execTypeDiv").hide();
                $(".timeExpDiv").hide();
            </c:if>
        });
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
    </ol>
    <h4 class="page-title">
        <i class="fa fa-edit" aria-hidden="true"></i>
        编辑作业
    </h4>

    <div style="float: right;margin-top: 5px">
        <a onclick="goback();" class="btn btn-sm m-t-10" style="margin-right: 16px;margin-bottom: -4px"><i class="fa fa-mail-reply" aria-hidden="true"></i>&nbsp;返回</a>
    </div>

    <div class="block-area" id="basic">
        <div class="tile p-15">
            <form class="form-horizontal" role="form" id="jobform" action="${contextPath}/job/edit.do" method="post" onsubmit="return save()"><br>
                <input type="hidden" name="csrf" value="${csrf}">
                <input type="hidden" id="jobId" name="jobId" value="${job.jobId}">
                <input type="hidden" name="command" id="command">
                <input type="hidden" name="userId" value="${job.userId}">
                <input type="hidden" id="agentId" name="agentId" class="input-self" value="${job.agentId}">

                <div class="form-group">
                    <label for="jobName" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;作业名称&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="jobName" name="jobName" value="${job.jobName}">
                        <span class="tips" tip="必填项,该作业的名称">必填项,该作业的名称</span>
                    </div>
                </div><br>

                <div class="form-group">
                    <label for="jobName" class="col-lab control-label wid150">
                        <i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;权重&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <select class="multiselect" title="jobGroupList" id="weight" name="weight">
                            <c:set var="weigthSet" value="false"/>
                            <c:forEach var="i" begin="0" end="5" step="1">
                                <c:if test="${weight==i}">
                                    <option value="${i}" selected>${i}</option>
                                    <c:set var="weigthSet" value="true"/>
                                </c:if>
                                <c:if test="${ weigthSet==false}">
                                    <option value="${i}">${i}</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="agentId" class="col-lab control-label wid150"><i class="glyphicon glyphicon-leaf"></i>&nbsp;&nbsp;执&nbsp;&nbsp;行&nbsp;&nbsp;器&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" value="${job.agentName}&nbsp;&nbsp;&nbsp;${job.ip}" readonly>
                        <font color="red">&nbsp;*只读</font>
                        <span class="tips">&nbsp;&nbsp;要执行此作业的机器名称和IP地址</span>
                    </div>
                </div><br>
                <div class="form-group">
                    <label for="groupName" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;分组名称：</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="groupName" value="${job.groupName}" readonly>
                        <font color="red">&nbsp;*只读</font>
                    </div>
                </div><br>

                <div class="form-group">
                    <label for="dependenceid" class="col-lab control-label wid150"><i class="glyphicon glyphicon-tasks"></i>&nbsp;&nbsp;前置依赖：</label>
                    <div class="col-md-10">
                        <select class="multiselect" title="依赖任务" multiple="multiple" id="dependenceid" name="dependenceid">
                            <c:forEach items="${jobs}" var="list">
                                <c:set var="flag" value="false"></c:set>
                                <c:if test="${!empty dependenceMap[list.jobId] }">
                                    <option  value="${list.jobId}"  selected="selected" >${list.jobName}</option>
                                    <c:set var="flag" value="true"></c:set>
                                </c:if>
                                <c:if test="${flag==false}">
                                    <option  value="${list.jobId}">${list.jobName}</option>
                                </c:if>
                            </c:forEach>
                        </select>
                        <button type="button" id="checkCycle" class="btn btn-primary">验证</button>
                    </div>

                </div><br>

                <div class="form-group execTypeDiv">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-info-sign"></i>&nbsp;&nbsp;运行模式&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="execType0" class="radio-label"><input type="radio" name="execType" id="execType0" value="0" ${job.execType eq 0 ? 'checked' : ''}>自动&nbsp;&nbsp;&nbsp;</label>
                        <label for="execType1" class="radio-label"><input type="radio" name="execType" id="execType1" value="1" ${job.execType eq 1 ? 'checked' : ''}>手动</label>&nbsp;&nbsp;&nbsp;
                        <br><span class="tips" id="execTypeTip" tip="">自动模式:执行器自动执行</span>
                    </div>
                </div><br>

                <div class="form-group cronExpDiv" style="display: ${job.execType eq 0 ? 'block' : 'none'}">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-bookmark"></i>&nbsp;&nbsp;规则类型&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="cronType0" class="radio-label"><input type="radio" name="cronType" value="0" id="cronType0" ${job.cronType eq 0 ? 'checked' : ''}>crontab&nbsp;&nbsp;&nbsp;</label>
                        <label for="cronType1" class="radio-label"><input type="radio" name="cronType" value="1" id="cronType1" ${job.cronType eq 1 ? 'checked' : ''}>quartz</label>&nbsp;&nbsp;&nbsp;
                        </br>
                        <span class="tips" id="cronTip" tip="crontab: unix/linux的时间格式表达式">
                             <c:if test="${job.cronType eq 0}">crontab: unix/linux的时间格式表达式 </c:if>
                             <c:if test="${job.cronType eq 1}">quartz: quartz框架的时间格式表达式</c:if>
                        </span>
                    </div>
                </div>
                <br>

                <div class="form-group cronExpDiv timeExpDiv" style="display: ${job.execType eq 0 ? 'block' : 'none'}">
                    <label for="cronExp" class="col-lab control-label wid150"><i class="glyphicon glyphicon-filter"></i>&nbsp;&nbsp;时间规则&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="cronExp" name="cronExp" value="${job.cronExp}">
                        </br>
                        <span class="tips" id="expTip" tip="crontab: unix/linux的时间格式表达式">
                            <c:if test="${job.cronType eq 0}">crontab: 请采用unix/linux的时间格式表达式,如 00 01 * * *</c:if>
                            <c:if test="${job.cronType eq 1}">quartz: 请采用quartz框架的时间格式表达式,如 0 0 10 L * ?</c:if>
                        </span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="cmd" class="col-lab control-label wid150"><i class="glyphicon glyphicon-th-large"></i>&nbsp;&nbsp;执行命令&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <textarea class="form-control input-sm" id="cmd" style="height:200px;resize:vertical">${job.command}</textarea>
                        <span class="tips" tip="请采用unix/linux的shell支持的命令">请采用unix/linux的shell支持的命令</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="runAs" class="col-lab control-label wid150"><i class="glyphicons glyphicons-user"></i>&nbsp;&nbsp;运行身份&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="runAs" name="runAs" value="${job.runAs}">
                        <span class="tips" tip="该任务以哪个身份执行(默认是root)">该任务以哪个身份执行(默认是root)</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="successExit" class="col-lab control-label wid150"><i class="glyphicons glyphicons-tags"></i>&nbsp;&nbsp;成功标识&nbsp;&nbsp;<b>*&nbsp;</b></label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="successExit" name="successExit" value="${job.successExit}">
                        <span class="tips" tip="自定义作业执行成功的返回标识(默认执行成功是0)">自定义作业执行成功的返回标识(默认执行成功是0)</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label class="col-lab control-label wid150"><i class="glyphicon  glyphicon glyphicon-forward"></i>&nbsp;&nbsp;失败重跑&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="redo01" class="radio-label"><input type="radio" name="redo" value="1" id="redo01" ${job.redo eq 1 ? 'checked' : ''}>是&nbsp;&nbsp;&nbsp;</label>
                        <label for="redo00" class="radio-label"><input type="radio" name="redo" value="0" id="redo00" ${job.redo eq 0 ? 'checked' : ''}>否</label>&nbsp;&nbsp;&nbsp;
                        <br><span class="tips">执行失败时是否自动重新执行</span>
                    </div>
                </div>
                <br>

                <div class="form-group countDiv" style="display: ${job.redo eq 1 ? 'block' : 'none'}">
                    <label for="runCount" class="col-lab control-label wid150"><i class="glyphicon glyphicon-repeat"></i>&nbsp;&nbsp;重跑次数&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="runCount" name="runCount" value="${job.runCount}">
                        <span class="tips" tip="执行失败时自动重新执行的截止次数">执行失败时自动重新执行的截止次数</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="timeout" class="col-lab control-label wid150"><i class="glyphicon glyphicon-ban-circle"></i>&nbsp;&nbsp;超时时间&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="timeout" name="timeout" value="${job.timeout}">
                        <span class="tips" tip="执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)">执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label class="col-lab control-label wid150"><i class="glyphicon glyphicon-warning-sign"></i>&nbsp;&nbsp;失败报警&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <label for="warning1" class="radio-label"><input type="radio" name="warning" value="1" id="warning1" ${job.warning eq true ? 'checked' : ''}>是&nbsp;&nbsp;&nbsp;</label>
                        <label for="warning0" class="radio-label"><input type="radio" name="warning" value="0" id="warning0" ${job.warning eq false ? 'checked' : ''}>否</label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                        </br><span class="tips" tip="任务执行失败时是否发信息报警">任务执行失败时是否发信息报警</span>
                    </div>
                </div>
                <br>

                <div class="form-group contact">
                    <label for="mobiles" class="col-lab control-label wid150"><i class="glyphicon glyphicon-comment"></i>&nbsp;&nbsp;报警手机&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="mobiles" name="mobiles" value="${job.mobiles}">
                        <span class="tips" tip="任务执行失败时将发送短信给此手机,多个请以逗号(英文)隔开">任务执行失败时将发送短信给此手机,多个请以逗号(英文)隔开</span>
                    </div>
                </div>
                <br>

                <div class="form-group contact" style="display: ${job.warning eq true ? 'block' : 'none'}">
                    <label for="email" class="col-lab control-label wid150"><i class="glyphicon glyphicon-envelope"></i>&nbsp;&nbsp;报警邮箱&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="email" name="emailAddress" value="${job.emailAddress}">
                        <span class="tips" tip="任务执行失败时将发送报告给此邮箱,多个请以逗号(英文)隔开">任务执行失败时将发送报告给此邮箱,多个请以逗号(英文)隔开</span>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <label for="comment" class="col-lab control-label wid150"><i class="glyphicon glyphicon-magnet"></i>&nbsp;&nbsp;描&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;述&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</label>
                    <div class="col-md-10">
                        <textarea class="form-control input-sm" id="comment" name="comment" style="height: 50px;">${job.comment}</textarea>
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

    <%--添加流程作业弹窗--%>
    <div class="modal fade" id="jobModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button class="close btn-float" data-dismiss="modal" aria-hidden="true"><i class="md md-close"></i>
                    </button>
                    <h4 id="subTitle" action="add" tid="">添加子作业</h4>
                </div>
                <div class="modal-body">
                    <form class="form-horizontal" role="form" id="subForm"><br>

                        <input type="hidden" id="itemRedo" value="1"/>
                        <div class="form-group">
                            <label for="agentId1" class="col-lab control-label wid100" title="要执行此作业的机器名称和IP地址">执&nbsp;&nbsp;行&nbsp;&nbsp;器&nbsp;&nbsp;&nbsp;</label>
                            <div class="col-md-9">
                                <select id="agentId1" name="agentId1" class="form-control m-b-10 ">
                                    <c:forEach var="d" items="${agents}">
                                        <option value="${d.agentId}">${d.ip}&nbsp;(${d.name})</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="jobName1" class="col-lab control-label wid100" title="作业名称必填">作业名称&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="jobName1">
                                <span class="tips" tip="必填项,该作业的名称">必填项,该作业的名称</span>
                            </div>
                        </div>



                        <div class="form-group">
                            <label for="cmd1" class="col-lab control-label wid100" title="请采用unix/linux的shell支持的命令">执行命令&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <textarea class="form-control" id="cmd1" name="cmd1" style="height:100px;resize:vertical"></textarea>
                                <span class="tips" tip="请采用unix/linux的shell支持的命令">请采用unix/linux的shell支持的命令</span>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="runAs1" class="col-lab control-label wid100">运行身份&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="runAs1" name="runAs1" value="root">
                                <span class="tips" tip="该任务以哪个身份执行(默认是root)">该任务以哪个身份执行(默认是root)</span>
                            </div>
                        </div>
                        <br>

                        <div class="form-group">
                            <label for="successExit1" class="col-lab control-label wid100">成功标识&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="successExit1" name="successExit1" value="0">
                                <span class="tips" tip="自定义作业执行成功的返回标识(默认执行成功是0)">自定义作业执行成功的返回标识(默认执行成功是0)</span>
                            </div>
                        </div>
                        <br>

                        <div class="form-group">
                            <label class="col-lab control-label wid100" title="执行失败时是否自动重新执行">失败重跑&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <label for="redo1" class="radio-label"><input type="radio" name="itemRedo" id="redo1" checked> 是&nbsp;&nbsp;&nbsp;</label>
                            <label for="redo0" class="radio-label"><input type="radio" name="itemRedo" id="redo0">否</label><br>
                        </div>
                        <br>
                        <div class="form-group countDiv1">
                            <label for="runCount1" class="col-lab control-label wid100" title="执行失败时自动重新执行的截止次数">重跑次数&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="runCount1"/>
                                <span class="tips" tip="执行失败时自动重新执行的截止次数">执行失败时自动重新执行的截止次数</span>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="timeout1" class="col-lab control-label wid100">超时时间&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="timeout1" value="0">
                                <span class="tips" tip="执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)">执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)</span>
                            </div>
                        </div>
                        <br>

                        <div class="form-group">
                            <label for="comment1" class="col-lab control-label wid100" title="此作业内容的描述">描&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;述&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="comment1"/>&nbsp;
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <center>
                        <button type="button" class="btn btn-sm" id="subjob-btn">保存</button>&nbsp;&nbsp;
                        <button type="button" class="btn btn-sm" data-dismiss="modal">关闭</button>
                    </center>
                </div>
            </div>
        </div>
    </div>

</section>

</body>

</html>

