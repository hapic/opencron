<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron"  uri="http://www.opencron.org"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html lang="en">
<head></head>

<style type="text/css">
    #cy {
        height: 600px;
        width: 900px;

        background: none
    }
    .none {
        text-align: center;
    }
</style>


<script type="text/javascript">
    $(function(){
        var cy=cytoscape({
            container: document.getElementById('cy'),
            autounselectify: true,
            style:cytoscape.stylesheet()
                .selector('node')
                .css({
                    'background-fit': 'cover',
                    'border-color': '#000',
                    'border-width': 3,
                    'border-opacity': 0.5,
                    'content': 'data(name)'
                })
                .selector('edge')
                .css({
                    'curve-style': 'bezier',
                    'width': 2,
                    'target-arrow-shape': 'triangle',
                    'line-color': '#000',
                    'target-arrow-color': '#000'
                })

            ,
            elements: {
                nodes: [
                    {data: {id: '2', name: '起始任务', label: '起始任务'}},
                    {data: {id: '3', name: '第二个任务', label: '第二个任务'}},
                    {data: {id: '1', name: '并行任务A', label: '并行任务A'}},
                    {data: {id: '4', name: '并行任务B', label: '并行任务B'}},
                    {data: {id: '5', name: '后续任务', label: '后续任务'}},
                    {data: {id: '6', name: '最终任务', label: '最终任务'}},
                    {data: {id: '8', name: '最终任务', label: '最终任务'}},
                    {data: {id: '7', name: '最终任务2', label: '最终任务'}}
                ],
                edges: [
                    {data:{source: '2', target: '3'}},
                    {data:{source: '3', target: '1'}},
                    {data:{source: '3', target: '4'}},
                    {data:{source: '1', target: '5'}},
                    {data:{source: '4', target: '5'}},
                    {data:{source: '5', target: '6'}},
                    {data:{source: '7', target: '8'}}
                ]
            },
            layout: { name: 'breadthfirst',directed: true,padding: 10}
        });
    });
</script>
<body>
<!-- Content -->
<section id="content" class="container">

    <!-- Messages Drawer -->
    <jsp:include page="/WEB-INF/layouts/message.jsp"/>
    <div class="row">
        <div class="col-md-12">
            <!-- overview -->
            <div class="tile " style="background: none">
                <h2 class="tile-title" style="width: 100%;background:rgba(0,0,0,0.40);border-top-left-radius:2px;border-top-right-radius:2px;"><i aria-hidden="true" class="fa fa-area-chart"></i>&nbsp;依赖关系图</h2>

                <div id="cy"></div>

            </div>
        </div>
    </div>


</section>

</body>
</html>
