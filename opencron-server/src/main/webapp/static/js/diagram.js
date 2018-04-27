
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
            setsChildPortSpot : false, // keep Spot.AllSides
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

function fillNoneDiagramData(div,modules) {
    var $G = go.GraphObject.make;
    this.$G = $G;
    myDiagram = $(go.Diagram, div,  // Must be the ID or reference to div
        {
            initialContentAlignment: go.Spot.Center,
            "undoManager.isEnabled": true
        });

    for (var i = 0; i < 15; i++) {
        myDiagram.add(
            $(go.Node,
                { position: new go.Point(Math.random() * 251, Math.random() * 251) },
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
                        new go.Binding("text", "name"))
                )
            ));
    }
    myDiagram.model = new go.GraphLinksModel(modules);

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