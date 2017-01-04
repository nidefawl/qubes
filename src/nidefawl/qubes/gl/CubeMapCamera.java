package nidefawl.qubes.gl;

import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Project;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;

public class CubeMapCamera {
	private BufferedMatrix projection;
	private BufferedMatrix view;
	private BufferedMatrix viewprojection;
	private BufferedMatrix modelviewprojection;
	private Matrix4f modelviewprojectionInv;
	private BufferedMatrix modelview;
	private BufferedMatrix normalMatrix;
	private BufferedMatrix identity;

	public void init() {
		projection = new BufferedMatrix();
		view = new BufferedMatrix();
		viewprojection = new BufferedMatrix();
		modelview = new BufferedMatrix();
		modelviewprojection = new BufferedMatrix();
		modelviewprojectionInv = new Matrix4f();
		normalMatrix = new BufferedMatrix();
		identity = new BufferedMatrix();
		identity.setIdentity();
		identity.update();
		identity.update();

	}

	public void setupScene(int side, Vector3f cameraPos) {
        final float fieldOfView = 90;
        final float aspectRatio = 1;
        final float znear = 0.1F;
        final float zfar = 1024F;
        Project.fovProjMat(fieldOfView, aspectRatio, znear, zfar, projection);
        projection.update();
        projection.update();
		view.setIdentity();
		switch (side) {
		case 0:
			view.rotate(0 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(-90 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		case 1:
			view.rotate(0 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(90 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		case 2:
			view.rotate(90 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(180 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		case 3:
			view.rotate(-90 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(180 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		case 4:
			view.rotate(0 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(180 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		case 5:
			view.rotate(0 * GameMath.PI_OVER_180, 1f, 0f, 0f);
			view.rotate(0 * GameMath.PI_OVER_180, 0f, 1f, 0f);
			break;
		}
		view.update();

		modelview.setIdentity();
		modelview.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
		modelview.translate(Engine.GLOBAL_OFFSET.x, 0, Engine.GLOBAL_OFFSET.z);
		// System.out.println(GLOBAL_OFFSET);

		Matrix4f.mul(view, modelview, modelview);
		Matrix4f.mul(projection, modelview, modelviewprojection);
		Matrix4f.mul(projection, view, viewprojection);
		viewprojection.update();
		modelview.update();
		modelviewprojection.update();
		normalMatrix.setIdentity();
		normalMatrix.invert().transpose();
		normalMatrix.update();
		Matrix4f.invert(modelviewprojection, modelviewprojectionInv);
		UniformBuffer.uboMatrix3D_Temp.reset();
		UniformBuffer.uboMatrix3D_Temp.put(modelviewprojection.get());// 0
		UniformBuffer.uboMatrix3D_Temp.put(modelview.get());// 1
		UniformBuffer.uboMatrix3D_Temp.put(view.get());// 2
		UniformBuffer.uboMatrix3D_Temp.put(viewprojection.get());// 3
		UniformBuffer.uboMatrix3D_Temp.put(projection.get());// 4
		UniformBuffer.uboMatrix3D_Temp.put(normalMatrix.get());// 5
		UniformBuffer.uboMatrix3D_Temp.put(modelview.getInv());// 6
		UniformBuffer.uboMatrix3D_Temp.put(projection.getInv());// 7
		UniformBuffer.uboMatrix3D_Temp.update();
	}
}
