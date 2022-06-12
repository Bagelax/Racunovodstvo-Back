package raf.si.racunovodstvo.nabavka.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import raf.si.racunovodstvo.nabavka.converters.IConverter;
import raf.si.racunovodstvo.nabavka.converters.impl.KalkulacijaConverter;
import raf.si.racunovodstvo.nabavka.converters.impl.KalkulacijaReverseConverter;
import raf.si.racunovodstvo.nabavka.model.Artikal;
import raf.si.racunovodstvo.nabavka.model.Kalkulacija;
import raf.si.racunovodstvo.nabavka.model.TroskoviNabavke;
import raf.si.racunovodstvo.nabavka.model.KalkulacijaArtikal;
import raf.si.racunovodstvo.nabavka.repositories.KalkulacijaRepository;
import raf.si.racunovodstvo.nabavka.requests.KalkulacijaRequest;
import raf.si.racunovodstvo.nabavka.responses.KalkulacijaResponse;
import raf.si.racunovodstvo.nabavka.services.IKalkulacijaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityNotFoundException;

@Service
public class KalkulacijaService implements IKalkulacijaService {

    private final KalkulacijaRepository kalkulacijaRepository;
    private final IConverter<KalkulacijaRequest, Kalkulacija> kalkulacijaConverter;
    private final IConverter<Kalkulacija, KalkulacijaResponse> kalkulacijaReverseConverter;

    public KalkulacijaService(KalkulacijaRepository kalkulacijaRepository,
                              KalkulacijaConverter kalkulacijaConverter,
                              KalkulacijaReverseConverter kalkulacijaReverseConverter) {
        this.kalkulacijaRepository = kalkulacijaRepository;
        this.kalkulacijaConverter = kalkulacijaConverter;
        this.kalkulacijaReverseConverter = kalkulacijaReverseConverter;
    }

    @Override
    public Page<KalkulacijaResponse> findAll(Specification<Kalkulacija> spec, Pageable pageSort) {
        return kalkulacijaRepository.findAll(spec, pageSort).map(kalkulacijaReverseConverter::convert);
    }

    @Override
    public Page<Kalkulacija> findAllKalkulacije(Specification<Kalkulacija> spec, Pageable pageable) {
        return kalkulacijaRepository.findAll(spec, pageable);
    }

    @Override
    public Kalkulacija increaseNabavnaAndProdajnaCena(Long kalkulacijaId, Double nabavnaCena, Double prodajnaCena) {
        Optional<Kalkulacija> optionalKalkulacija = findById(kalkulacijaId);
        if (optionalKalkulacija.isEmpty()) {
            throw new EntityNotFoundException();
        }
        Kalkulacija kalkulacija = optionalKalkulacija.get();

        kalkulacija.setProdajnaCena(kalkulacija.getProdajnaCena() + prodajnaCena);
        Double ukupnaFakturnaCena = kalkulacija.getFakturnaCena() + nabavnaCena;
        kalkulacija.setFakturnaCena(ukupnaFakturnaCena);
        Double ukupniTroskoviNabavke = kalkulacija.getTroskoviNabavke().stream().mapToDouble(TroskoviNabavke::getCena).sum();
        kalkulacija.setNabavnaVrednost(ukupniTroskoviNabavke + ukupnaFakturnaCena);
        return kalkulacijaRepository.save(kalkulacija);
    }

    @Override
    public <S extends Kalkulacija> S save(S var1) {
        return kalkulacijaRepository.save(var1);
    }

    public KalkulacijaResponse save(KalkulacijaRequest kalkulacijaRequest) {
        Kalkulacija kalkulacija = kalkulacijaConverter.convert(kalkulacijaRequest);
        kalkulacija.calculateCene();
        return kalkulacijaReverseConverter.convert(kalkulacijaRepository.save(kalkulacija));
    }

    public KalkulacijaResponse update(KalkulacijaRequest kalkulacijaRequest) {
        Optional<Kalkulacija> optionalKalkulacija = this.kalkulacijaRepository.findById(kalkulacijaRequest.getId());
        if (optionalKalkulacija.isEmpty()) {
            throw new EntityNotFoundException();
        }
        return this.save(kalkulacijaRequest);
    }

    @Override
    public Optional<Kalkulacija> findById(Long var1) {
        return kalkulacijaRepository.findById(var1);
    }

    @Override
    public List<Kalkulacija> findAll() {
        return this.kalkulacijaRepository.findAll();
    }

    public Page<KalkulacijaResponse> findAll(Pageable pageable) {
        return kalkulacijaRepository.findAll(pageable).map(kalkulacijaReverseConverter::convert);
    }

    @Override
    public void deleteById(Long var1) {
        Optional<Kalkulacija> optionalKalkulacija = kalkulacijaRepository.findById(var1);
        if (optionalKalkulacija.isEmpty()) {
            throw new EntityNotFoundException();
        }
        kalkulacijaRepository.deleteById(var1);
    }

    @Override
    public Map<String, Number> getTotalKalkulacije(List<Kalkulacija> kalkulacije) {
        Map<String, Number> values = new HashMap<>();
        Integer totalKolicina = 0;
        Double totalRabat = 0.0;
        Double totalNabavnaCena = 0.0;
        Double totalNabavnaCenaPosleRabata = 0.0;
        Double totalNabavnaVrednost = 0.0;
        Double totalMarza = 0.0;
        Double totalOsnovicaZaProdaju = 0.0;
        Double totalPorez = 0.0;
        Double totalProdajnaCena = 0.0;
        Double totalPoreskaOsnovica = 0.0;
        Double totalProdajnaVrednost = 0.0;
        for (Kalkulacija kalkulacija : kalkulacije) {
            totalKolicina += kalkulacija.getArtikli().stream().map(Artikal::getKolicina).reduce(0, Integer::sum);
            totalRabat += kalkulacija.getArtikli().stream().map(Artikal::getRabat).reduce(0.0, Double::sum);
            totalNabavnaCena += kalkulacija.getArtikli().stream().map(Artikal::getNabavnaCena).reduce(0.0, Double::sum);
            totalNabavnaCenaPosleRabata += kalkulacija.getArtikli().stream().map(Artikal::getNabavnaCenaPosleRabata).reduce(0.0, Double::sum);
            totalNabavnaVrednost += kalkulacija.getArtikli().stream().map(Artikal::getUkupnaNabavnaVrednost).reduce(0.0, Double::sum);
            totalMarza += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getMarza).reduce(0.0, Double::sum);
            totalOsnovicaZaProdaju += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getProdajnaOsnovica).reduce(0.0, Double::sum);
            totalPorez += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getPorez).reduce(0.0, Double::sum);
            totalProdajnaCena += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getProdajnaCena).reduce(0.0, Double::sum);
            totalPoreskaOsnovica += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getOsnovica).reduce(0.0, Double::sum);
            totalProdajnaVrednost += kalkulacija.getArtikli().stream().map(KalkulacijaArtikal::getUkupnaProdajnaVrednost).reduce(0.0, Double::sum);
        }
        values.put("totalKolicina", totalKolicina);
        values.put("totalRabat", totalRabat);
        values.put("totalNabavnaCena", totalNabavnaCena);
        values.put("totalNabavnaCenaPosleRabata", totalNabavnaCenaPosleRabata);
        values.put("totalNabavnaVrednost", totalNabavnaVrednost);
        values.put("totalMarza", totalMarza);
        values.put("totalOsnovicaZaProdaju", totalOsnovicaZaProdaju);
        values.put("totalPorez", totalPorez);
        values.put("totalProdajnaCena", totalProdajnaCena);
        values.put("totalPoreskaOsnovica", totalPoreskaOsnovica);
        values.put("totalProdajnaVrednost", totalProdajnaVrednost);
        return values;
    }
}
