<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <link rel="stylesheet" href="${contextPath}/static/js/bootstrap-select/bootstrap-select.css">
    <script src="${contextPath}/static/js/bootstrap-select/bootstrap-select.js"></script>
    <script src="${contextPath}/static/js/bootstrap-select/bootstrap-select-lang.js"></script>

    <style type="text/css">
        .dropdown-menu {
            background: rgba(25, 25, 25, 0.98) none repeat scroll 0 0;
            color: #000;
            margin-top: -1px;
            overflow: hidden;
        }

        .dropdown-menu > li > a {
            color: rgba(195, 195, 195, 0.95);
            font-family: open-sans-regular;
        }

        .dropdown-menu > li > a:hover {
            color: #fff;
            background-color: rgba(0,0,0,0.95);
        }

        .dropdown-menu > .selected{
            background-color: rgba(20,20,20,20.90);
        }

        .dropdown-header {
            color: rgba(245, 245, 245, 0.95);
            display: block;
            font-size: 12px;
            line-height: 1.42857;
            padding: 3px 20px;
        }

    </style>
    <script type="text/javascript">

        function save() {
            var name = $("#groupName").val();
            if (!name) {
                $("#checkName").html("<font color='red'>" + '<i class="glyphicon glyphicon-remove-sign"></i>&nbsp;请填写组名称' + "</font>");
                return false;
            }
        }

        $(document).ready(function () {

            $('.selectpicker').selectpicker({
                style: 'btn-info'
            });

            $("#groupName").blur(function () {
                if (!$("#groupName").val()) {
                    $("#checkName").html("<font color='red'>" + '<i class="glyphicon glyphicon-remove-sign"></i>&nbsp;请填写组名称' + "</font>");
                    return false;
                }
                $.ajax({
                    headers: {"csrf": "${csrf}"},
                    type: "POST",
                    url: "${contextPath}/jobGroup/checkname.do",
                    data: {
                        "name": $("#groupName").val()
                    },
                    success: function (data) {
                        if (data) {
                            $("#checkName").html("<font color='green'>" + '<i class="glyphicon glyphicon-ok-sign"></i>&nbsp;作业组名称可用' + "</font>");
                            return false;
                        } else {
                            $("#checkName").html("<font color='red'>" + '<i class="glyphicon glyphicon-remove-sign"></i>&nbsp;作业组名称已存在' + "</font>");
                            return false;
                        }
                    },
                    error: function () {
                        alert("网络繁忙请刷新页面重试!");
                        return false;
                    }
                });
            }).focus(function () {
                $("#checkName").html('<b>*&nbsp;</b>任务组名称必填');
            });

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
        <li><a href="${contextPath}/jobGroup/view.htm?csrf=${csrf}">作业组列表</a></li>
        <li><a href=""> 添加作业组</a></li>
    </ol>
    <h4 class="page-title"><i aria-hidden="true" class="fa fa-plus"></i>&nbsp;添加任务分组</h4>

    <div style="float: right;margin-top: 5px">
        <a onclick="goback();" class="btn btn-sm m-t-10" style="margin-right: 16px;margin-bottom: -4px"><i
                class="fa fa-mail-reply" aria-hidden="true"></i>&nbsp;返回</a>
    </div>

    <div class="block-area" id="basic">
        <div class="tile p-15">
            <form class="form-horizontal" role="form" id="agent" action="${contextPath}/jobGroup/save.do" method="post" onsubmit="return save()"></br>
                <input type="hidden" name="csrf" value="${csrf}">
                <div class="form-group">
                    <label for="groupName" class="col-lab control-label"><i class="glyphicon glyphicon-leaf"></i>&nbsp;&nbsp;任务组名称：</label>
                    <div class="col-md-10">
                        <input type="text" class="form-control input-sm" id="groupName" name="name" />
                        <span class="tips" id="checkName"><b>*&nbsp;</b>任务组名称必填</span>
                    </div>
                </div>
                <br>

                <div class="form-group" style="margin-top: 25px;">
                    <label for="comment" class="col-lab control-label"><i class="glyphicon glyphicon-magnet"></i>&nbsp;&nbsp;描&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;述：</label>
                    <div class="col-md-10" style="margin-left: 2px;">
                        <textarea class="form-control input-sm" id="comment" name="comment"></textarea>
                    </div>
                </div>
                <br>

                <div class="form-group">
                    <div class="col-md-offset-1 col-md-10">
                        <button type="submit" class="btn btn-sm m-t-10"><i class="icon">&#61717;</i>&nbsp;保存
                        </button>&nbsp;&nbsp;
                        <button type="button" onclick="history.back()" class="btn btn-sm m-t-10"><i class="icon">&#61740;</i>&nbsp;取消
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

</section>

</body>

</html>

