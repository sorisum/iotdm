define(['iotdm-gui.controllers.module'], function(app) {
    'use strict';

    function SidePanelInfoCtrl($scope,DataStore,TopologyHelper,Onem2m, Onem2mDescription) {
        var _this = this;
        var descriptions = {};

        _this.root = {};
        _this.path = [];

        _this.ancestor = ancestor;
        _this.children = children;
        _this.parent = parent;
        _this.yourName = yourName;
        _this.yourself = yourself;
        _this.isValue = isValue;
        _this.isRoot = isRoot;
        _this.isArray = isArray;
        _this.description = description;

        init();

        function init() {
            $scope.$on('selectNode',function(event,id){
                reset(DataStore.retrieveNode(id));
                $scope.$apply();
            });
            reset(TopologyHelper.getSelectedNode());
        }

        function reset(node) {
          if(node){
            _this.root = {};
            _this.path = [];
            _this.root[node.key] = node.value;
            _this.path.push(node.key);

            var resourceType = node.value.ty;
            descriptions = Onem2mDescription.descriptionByResourceType(resourceType);
          }
        }

        function ancestor(index) {
            _this.path.splice(index + 1);
        }

        function children(name) {
            _this.path.push(name);
        }

        function parent() {
            _this.path.pop();
        }

        function yourName() {
            return _this.path.slice(-1)[0];
        }

        function yourself() {
            var place = _this.root;
            _this.path.forEach(function(p) {
                place = place[p];
            });
            return place;
        }

        function isValue(value) {
            return !angular.isObject(value);
        }

        function isRoot() {
            return _this.path.length == 1;
        }

        function isArray(array) {
            return angular.isArray(array);
        }

        function description(name) {
            return descriptions[name];
        }
    }

    SidePanelInfoCtrl.$inject = ['$scope', 'DataStoreService', 'TopologyHelperService','Onem2mHelperService', 'Onem2mDescriptionService'];
    app.controller('SidePanelInfoCtrl', SidePanelInfoCtrl);
});
