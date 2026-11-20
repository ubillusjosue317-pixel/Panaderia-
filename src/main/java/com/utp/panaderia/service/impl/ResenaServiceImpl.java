package com.utp.panaderia.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.dto.ResenaForm;
import com.utp.panaderia.model.Resena;
import com.utp.panaderia.repository.ResenaRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.ProductoService;
import com.utp.panaderia.service.ResenaService;

@Service
public class ResenaServiceImpl implements ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public List<Resena> listar(Integer idProducto) {
        return resenaRepository.listarDelProducto(idProducto);
    }

    @Override
    @Transactional(readOnly = true)
    public Double promedio(Integer idProducto) {
        return resenaRepository.promedio(idProducto);
    }

    @Override
    @Transactional
    public Resena guardar(Integer idUsuario, Integer idProducto, ResenaForm form) {
        Resena resena = resenaRepository.findByProductoIdAndUsuarioId(idProducto, idUsuario).orElseGet(() -> {
            Resena nueva = new Resena();
            nueva.setProducto(productoService.buscarPorId(idProducto));
            nueva.setUsuario(usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado")));
            return nueva;
        });
        resena.setCalificacion(form.getCalificacion());
        // el texto se guarda tal cual lo escribio (sin caracteres de control); al mostrarlo la
        // plantilla lo escapa, asi que un "<script>" se ve como texto y nunca se ejecuta
        resena.setComentario(form.getComentario().replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim());
        return resenaRepository.save(resena);
    }

    @Override
    @Transactional
    public void eliminar(Long idResena, Integer idUsuario, boolean esAdmin) {
        Resena resena = resenaRepository.findById(idResena)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa reseña."));
        if (!esAdmin && !resena.getUsuario().getId().equals(idUsuario)) {
            throw new AccessDeniedException("Solo puedes borrar tus propias reseñas");
        }
        Integer idProducto = resena.getProducto().getId();
        resenaRepository.delete(resena);
        if (esAdmin) {
            auditoriaService.registrar("ELIMINAR_RESENA", "Resena", idProducto, "Reseña #" + idResena + " eliminada por moderación");
        }
    }
}
