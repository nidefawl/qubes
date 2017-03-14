package nidefawl.qubes.models.render.impl.vk;

import nidefawl.qubes.gl.BufferedMatrix;
import nidefawl.qubes.models.render.QModelBatchedRender;

public class QModelBatchedRenderVK extends QModelBatchedRender {

    @Override
    public void render(float fTime) {
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }

    @Override
    public void sync() {
    }

    @Override
    public void initShaders() {
    }

    @Override
    public void setForwardRenderMVP(BufferedMatrix mvp) {
        this.mvp=mvp;
    }

}
