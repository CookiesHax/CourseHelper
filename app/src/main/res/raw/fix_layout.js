(function() {
    // 设置地图容器高度
    var dituContent = document.getElementById('dituContent');
    if (dituContent) {
        var footer = document.querySelector('.zgetSite_footer');
        var footerHeight = footer ? footer.offsetHeight : 0;
        var rect = dituContent.getBoundingClientRect();
        var availableHeight = window.innerHeight - rect.top - footerHeight - totalOffset;
        if (availableHeight > 0) {
            dituContent.style.height = availableHeight + 'px';
        }
    }

    // 如果地图已初始化 触发重算
    if (window.map) {
        window.dispatchEvent(new Event('resize'));
    }
})();
