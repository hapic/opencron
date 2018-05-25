
function fillDiagramData(div,modules,moduleshierarchy) {
    var $G = go.GraphObject.make;
    this.$G = $G;
    var diagram = $G(go.Diagram, div, {
        initialContentAlignment : go.Spot.Left, // center Diagram contents
        "undoManager.isEnabled" : true,
        doubleClick : function() {
            console.log('doubleClick');
        },
        layout : $G(go.TreeLayout, { // this only lays out in trees nodes
            setsPortSpot : false, // keep Spot.AllSides for link connection
            setsChildPortSpot : true, // keep Spot.AllSides
        })

    });
    this.diagram = diagram;

    diagram.nodeTemplate =$G(go.Node, "Auto",  // the Shape will go around the TextBlock
        $G(go.Shape, "RoundedRectangle",
            { strokeWidth: 0 },
            new go.Binding("fill", "color")),
        $G(go.Panel, "Table", {
                defaultAlignment : go.Spot.Left,
                margin : 4
            },

            $G(go.TextBlock,{row : 0,column : 0, columnSpan : 3,alignment : go.Spot.Center},
                {font : "bold 12pt sans-serif" },
                { margin: 4},  // some room around the text
                // TextBlock.text is bound to Node.data.key
                new go.Binding("text", "name"))
        )
    );
    diagram.model = new go.GraphLinksModel(modules, moduleshierarchy);

}

function fillDiagramData2(div,modules,moduleshierarchy){
    var $G = go.GraphObject.make;

    var diagram =$G(go.Diagram, div,
        {
            initialContentAlignment: go.Spot.Center,
            "animationManager.isEnabled": false
        });
    diagram.nodeTemplate =$G(go.Node, "Auto",
        $G(go.Shape, "RoundedRectangle",
            { strokeWidth: 0},
            new go.Binding("fill", "color")
        ),
        $G(go.TextBlock, {font : "bold 12pt sans-serif" },{ margin: 5 },
            new go.Binding("text", "name"))
    );
    diagram.linkTemplate =$G(go.Link,
        { relinkableFrom: true, relinkableTo: true },
        $G(go.Shape),
        $G(go.Shape, { toArrow: "Standard" })
    );

    function updateCrossLaneLinks(group) {
        group.findExternalLinksConnected().each(function(l) {
            l.visible = (l.fromNode.isVisible() && l.toNode.isVisible());
        });
    }
    function relayoutLanes() {
        diagram.nodes.each(function(lane) {
            if (!(lane instanceof go.Group)) return;
            if (lane.category === "Pool") return;
            lane.layout.isValidLayout = false;  // force it to be invalid
        });
        diagram.layoutDiagram();
    }

    diagram.groupTemplate =
        $G(go.Group, "Horizontal",
            {
                selectionObjectName: "SHAPE",  // selecting a lane causes the body of the lane to be highlit, not the label
                resizable: true, resizeObjectName: "SHAPE",  // the custom resizeAdornmentTemplate only permits two kinds of resizing
                layout: $G(go.LayeredDigraphLayout,  // automatically lay out the lane's subgraph
                    {
                        isInitial: false,  // don't even do initial layout
                        isOngoing: false,  // don't invalidate layout when nodes or links are added or removed
                        direction: 0,
                        columnSpacing: 10,
                        layeringOption: go.LayeredDigraphLayout.LayerLongestPathSource
                    }),
                computesBoundsAfterDrag: true,  // needed to prevent recomputing Group.placeholder bounds too soon
                computesBoundsIncludingLinks: false,  // to reduce occurrences of links going briefly outside the lane
                computesBoundsIncludingLocation: true,  // to support empty space at top-left corner of lane
                handlesDragDropForMembers: true,  // don't need to define handlers on member Nodes and Links
                mouseDrop: function(e, grp) {  // dropping a copy of some Nodes and Links onto this Group adds them to this Group
                    if (!e.shift) return;  // cannot change groups with an unmodified drag-and-drop
                    // don't allow drag-and-dropping a mix of regular Nodes and Groups
                    if (!e.diagram.selection.any(function(n) { return n instanceof go.Group; })) {
                        var ok = grp.addMembers(grp.diagram.selection, true);
                        if (ok) {
                            updateCrossLaneLinks(grp);
                        } else {
                            grp.diagram.currentTool.doCancel();
                        }
                    } else {
                        e.diagram.currentTool.doCancel();
                    }
                },
                subGraphExpandedChanged: function(grp) {
                    var shp = grp.resizeObject;
                    if (grp.diagram.undoManager.isUndoingRedoing) return;
                    if (grp.isSubGraphExpanded) {
                        shp.height = grp._savedBreadth;
                    } else {
                        grp._savedBreadth = shp.height;
                        shp.height = NaN;
                    }
                    updateCrossLaneLinks(grp);
                }
            }

        )
    diagram.model = new go.GraphLinksModel(modules, moduleshierarchy);
    relayoutLanes();
}



function fillCycleDiagramData(div,modules) {
    var $G = go.GraphObject.make;
    this.$G = $G;
    var diagram = $G(go.Diagram, div);
    this.diagram = diagram;

    diagram.nodeTemplate =$G(go.Node, "Auto",  // the Shape will go around the TextBlock
        $G(go.Shape, "RoundedRectangle",
            { strokeWidth: 0 },
            new go.Binding("fill", "color")),
        $G(go.Panel, "Table", {
                defaultAlignment : go.Spot.Left,
                margin : 4
            },
            $G(go.TextBlock,{row : 0,column : 0, columnSpan : 3,alignment : go.Spot.Center},
                {font : "bold 12pt sans-serif" },
                { margin: 4},  // some room around the text
                // TextBlock.text is bound to Node.data.key
                new go.Binding("text", "name"))
        )
    );
    diagram.model = new go.GraphLinksModel(modules);
}
